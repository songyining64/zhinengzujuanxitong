package com.example.exam.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.common.api.ErrorCode;
import com.example.exam.common.enums.RoleEnum;
import com.example.exam.module.system.dto.RegisterRequest;
import com.example.exam.common.exception.BizException;
import com.example.exam.module.system.dto.UserCreateRequest;
import com.example.exam.module.system.dto.UserUpdateRequest;
import com.example.exam.module.system.entity.User;
import com.example.exam.module.system.mapper.UserMapper;
import com.example.exam.module.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
    public User findById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public Page<User> pageUsers(String keyword, String role, long current, long size) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getRealName, keyword));
        }
        if (role != null && !role.isBlank()) {
            wrapper.eq(User::getRole, role.trim());
        }
        Page<User> page = new Page<>(current, size);
        userMapper.selectPage(page, wrapper.orderByDesc(User::getId));
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
    @Transactional(rollbackFor = Exception.class)
    public User createUser(UserCreateRequest req) {
        long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername()));
        if (exists > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "用户名已存在");
        }
        User u = new User();
        u.setUsername(req.getUsername());
        u.setPassword(req.getPassword());
        u.setRealName(req.getRealName());
        try {
            RoleEnum.valueOf(req.getRole().trim());
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, "无效的角色");
        }
        u.setRole(req.getRole().trim());
        u.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        u.setCreateTime(LocalDateTime.now());
        u.setUpdateTime(LocalDateTime.now());
        saveUser(u);
        return u;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User updateUser(Long id, UserUpdateRequest req) {
        User u = userMapper.selectById(id);
        if (u == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (req.getRealName() != null) {
            u.setRealName(req.getRealName());
        }
        if (req.getRole() != null) {
            try {
                RoleEnum.valueOf(req.getRole().trim());
            } catch (IllegalArgumentException e) {
                throw new BizException(ErrorCode.BAD_REQUEST, "无效的角色");
            }
            u.setRole(req.getRole().trim());
        }
        if (req.getStatus() != null) {
            u.setStatus(req.getStatus());
        }
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            u.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        u.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(u);
        return u;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerStudent(RegisterRequest req) {
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "两次输入的密码不一致");
        }
        String name = req.getUsername().trim();
        long exists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, name));
        if (exists > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "用户名已被注册");
        }
        User u = new User();
        u.setUsername(name);
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setRealName(req.getRealName() != null && !req.getRealName().isBlank() ? req.getRealName().trim() : null);
        u.setRole(RoleEnum.STUDENT.name());
        u.setStatus(1);
        u.setCreateTime(LocalDateTime.now());
        u.setUpdateTime(LocalDateTime.now());
        userMapper.insert(u);
    }
}

