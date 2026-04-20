package com.example.exam;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.common.enums.RoleEnum;
import com.example.exam.module.system.entity.User;
import com.example.exam.config.AppProperties;
import com.example.exam.config.AppRedisProperties;
import com.example.exam.config.ExamProperties;
import com.example.exam.config.JwtProperties;
import com.example.exam.module.system.service.UserService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.LocalDateTime;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties({JwtProperties.class, AppProperties.class, ExamProperties.class, AppRedisProperties.class})
public class ExamApplication {

    private final UserService userService;

    public ExamApplication(UserService userService) {
        this.userService = userService;
    }

    public static void main(String[] args) {
        SpringApplication.run(ExamApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initDemoUsers() {
        ensureUser("admin", "admin123", "系统管理员", RoleEnum.ADMIN);
        ensureUser("teacher", "teacher123", "演示教师", RoleEnum.TEACHER);
        ensureUser("student", "student123", "演示学生", RoleEnum.STUDENT);
    }

    private void ensureUser(String username, String password, String realName, RoleEnum role) {
        Page<User> page = userService.pageUsers(username, null, 1, 1);
        boolean exists = page.getRecords().stream().anyMatch(u -> username.equals(u.getUsername()));
        if (!exists) {
            User u = new User();
            u.setUsername(username);
            u.setPassword(password);
            u.setRealName(realName);
            u.setRole(role.name());
            u.setStatus(1);
            u.setCreateTime(LocalDateTime.now());
            u.setUpdateTime(LocalDateTime.now());
            userService.saveUser(u);
        }
    }
}
