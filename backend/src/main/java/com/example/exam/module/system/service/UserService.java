package com.example.exam.module.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.module.system.entity.User;

public interface UserService {

    User findByUsername(String username);

    Page<User> pageUsers(String keyword, long current, long size);

    User saveUser(User user);

    /**
     * 新建用户（明文密码会在内部加密）；用户名已存在时抛出业务异常。
     */
    User createUser(String username, String rawPassword, String realName, String role, Integer status);

    /** 删除用户（可根据需要做安全校验） */
    void deleteUser(Long id);
}

