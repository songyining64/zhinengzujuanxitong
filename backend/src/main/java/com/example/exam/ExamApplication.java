package com.example.exam;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.common.enums.RoleEnum;
import com.example.exam.module.system.entity.User;
import com.example.exam.module.system.service.UserService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.time.LocalDateTime;

@SpringBootApplication
public class ExamApplication {

    private final UserService userService;

    public ExamApplication(UserService userService) {
        this.userService = userService;
    }

    public static void main(String[] args) {
        SpringApplication.run(ExamApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initAdminUser() {
        Page<User> page = userService.pageUsers("admin", 1, 1);
        boolean exists = page.getRecords().stream().anyMatch(u -> "admin".equals(u.getUsername()));
        if (!exists) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword("admin123");
            admin.setRealName("系统管理员");
            admin.setRole(RoleEnum.ADMIN.name());
            admin.setStatus(1);
            admin.setCreateTime(LocalDateTime.now());
            admin.setUpdateTime(LocalDateTime.now());
            userService.saveUser(admin);
        }
    }
}
