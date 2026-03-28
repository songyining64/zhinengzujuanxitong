package com.example.exam.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.common.api.ErrorCode;
import com.example.exam.common.enums.RoleEnum;
import com.example.exam.common.exception.BizException;
import com.example.exam.module.system.dto.UserCreateRequest;
import com.example.exam.module.system.entity.User;
import com.example.exam.module.system.mapper.UserMapper;
import com.example.exam.module.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
    }

    @Override
    public Page<User> pageUsers(String keyword, long current, long size) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
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
    public User createUser(UserCreateRequest req) {
        String username = req.getUsername().trim();
        if (findByUsername(username) != null) {
            throw new BizException(ErrorCode.DUPLICATE, "用户名已存在");
        }
        RoleEnum r;
        try {
            r = RoleEnum.valueOf(req.getRole().trim());
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, "角色无效，应为 ADMIN / TEACHER / STUDENT");
        }
        User u = new User();
        u.setUsername(username);
        u.setPassword(req.getPassword());
        u.setRealName(req.getRealName());
        u.setRole(r.name());
        u.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        return saveUser(u);
    }
}

