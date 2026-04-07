package com.example.exam.module.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.module.system.entity.User;

public interface UserService {

    User findByUsername(String username);

    Page<User> pageUsers(String keyword, long current, long size);

    /** 仅启用状态的学生账号，供选课等场景检索 */
    Page<User> pageStudents(String keyword, long current, long size);

    User saveUser(User user);
}

