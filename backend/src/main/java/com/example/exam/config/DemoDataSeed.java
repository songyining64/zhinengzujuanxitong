package com.example.exam.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 在菜单种子之后写入演示用户与业务数据（仅当对应表为空）。
 */
@Component
@RequiredArgsConstructor
public class DemoDataSeed {

    private final DemoDataInitializerService demoDataInitializerService;

    @Order(150)
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        demoDataInitializerService.ensureDemoUsers();
        demoDataInitializerService.ensureDemoCourseChain();
    }
}
