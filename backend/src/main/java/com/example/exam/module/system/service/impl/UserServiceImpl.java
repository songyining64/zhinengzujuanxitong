package com.example.exam.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.common.api.ErrorCode;
import com.example.exam.common.enums.RoleEnum;
import com.example.exam.common.exception.BizException;
import com.example.exam.common.security.SecurityHelper;
import com.example.exam.module.system.entity.User;
import com.example.exam.module.system.mapper.UserMapper;
import com.example.exam.module.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String DELETED_USERNAME_PREFIX = "deleted_";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User findByUsername(String username) {
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (u != null && u.getRole() != null) {
            u.setRole(u.getRole().trim());
        }
        return u;
    }

    @Override
    public Page<User> pageUsers(String keyword, long current, long size) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .notLikeRight(User::getUsername, DELETED_USERNAME_PREFIX);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(User::getUsername, keyword)
                    .or()
                    .like(User::getRealName, keyword);
        }
        Page<User> page = new Page<>(current, size);
        userMapper.selectPage(page, wrapper);
        return page;
    }

    @Override
    public User saveUser(User user) {
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        if (user.getId() == null) {
            userMapper.insert(user);
        } else {
            userMapper.updateById(user);
        }
        return user;
    }

    @Override
    public User createUser(String username, String rawPassword, String realName, String role, Integer status) {
        String u = username != null ? username.trim() : "";
        if (u.isEmpty()) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "用户名不能为空");
        }
        if (findByUsername(u) != null) {
            throw new BizException(ErrorCode.DUPLICATE, "用户名已存在");
        }
        RoleEnum roleEnum;
        try {
            roleEnum = RoleEnum.valueOf(role);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "角色不合法");
        }
        User user = new User();
        user.setUsername(u);
        user.setPassword(rawPassword);
        user.setRealName(realName != null && !realName.isBlank() ? realName.trim() : null);
        user.setRole(roleEnum.name());
        int st = (status != null && status == 0) ? 0 : 1;
        user.setStatus(st);
        LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);
        saveUser(user);
        user.setPassword(null);
        return user;
    }

    @Override
    public void deleteUser(Long id) {
        User existing = userMapper.selectById(id);
        if (existing == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        String currentUsername = SecurityHelper.requireUsername();
        if (existing.getUsername() != null && existing.getUsername().equals(currentUsername)) {
            throw new BizException(ErrorCode.FORBIDDEN, "不能删除当前登录用户");
        }
        // 逻辑删除：保留历史关联（考试记录、课程关系等），仅禁用并从列表隐藏
        String tombstoneUsername = DELETED_USERNAME_PREFIX + existing.getId() + "_" + (System.currentTimeMillis() % 1_000_000);
        existing.setUsername(tombstoneUsername);
        existing.setStatus(0);
        String realName = existing.getRealName() == null ? "" : existing.getRealName().trim();
        existing.setRealName(realName.isEmpty() ? "已删除用户" : realName + "(已删除)");
        existing.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(existing);
    }
}

