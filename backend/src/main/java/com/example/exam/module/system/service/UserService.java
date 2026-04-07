package com.example.exam.module.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.module.system.dto.RegisterRequest;
import com.example.exam.module.system.dto.UserCreateRequest;
import com.example.exam.module.system.dto.UserUpdateRequest;
import com.example.exam.module.system.entity.User;

public interface UserService {

    User findByUsername(String username);

    User findById(Long id);

    Page<User> pageUsers(String keyword, String role, long current, long size);

    /** 仅启用状态的学生账号，供选课等场景检索 */
    Page<User> pageStudents(String keyword, long current, long size);

    User saveUser(User user);

    User createUser(UserCreateRequest req);

    User updateUser(Long id, UserUpdateRequest req);

    /** 自助注册为学生（公开接口） */
    void registerStudent(RegisterRequest req);
}

