package com.example.exam.module.system.controller;

import com.example.exam.common.api.ApiResponse;
import com.example.exam.module.system.dto.MenuTreeVO;
import com.example.exam.module.system.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService sysMenuService;

    @GetMapping("/tree")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<MenuTreeVO>> tree() {
        return ApiResponse.success(sysMenuService.treeForCurrentRole());
    }
}
