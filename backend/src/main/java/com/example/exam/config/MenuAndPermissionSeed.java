package com.example.exam.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.exam.common.enums.RoleEnum;
import com.example.exam.module.system.entity.SysMenu;
import com.example.exam.module.system.entity.SysRoleMenu;
import com.example.exam.module.system.mapper.SysMenuMapper;
import com.example.exam.module.system.mapper.SysRoleMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 初始化菜单与角色-菜单绑定，使 sys_menu.perms 与接口 {@code hasAuthority} 一致。
 * 合并为单一路径，避免同一 path 出现多条菜单导致前端高亮异常。
 */
@Component
@RequiredArgsConstructor
public class MenuAndPermissionSeed {

    private static final Set<String> TEACHER_PERMS = Set.of(
            "course:read", "course:manage",
            "knowledge:manage", "knowledge:read",
            "question:manage", "question:read",
            "paper:manage", "paper:read",
            "exam:manage", "exam:analytics"
    );

    private static final Set<String> STUDENT_PERMS = Set.of(
            "course:read",
            "knowledge:read",
            "question:read",
            "paper:read",
            "exam:student",
            "exam:analytics",
            "wrongbook:read"
    );

    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;

    @Order(100)
    @EventListener(ApplicationReadyEvent.class)
    public void seedMenusAndRoleMenus() {
        long n = sysMenuMapper.selectCount(new LambdaQueryWrapper<SysMenu>());
        if (n > 0) {
            return;
        }
        List<SysMenu> menus = new ArrayList<>();
        menus.add(menu("用户管理", "system/user", "system:user:manage", 10));
        menus.add(menu("课程中心", "course", "course:read,course:manage", 20));
        menus.add(menu("知识点", "knowledge", "knowledge:read,knowledge:manage", 30));
        menus.add(menu("题库", "question", "question:read,question:manage", 40));
        menus.add(menu("试卷与组卷", "paper", "paper:read,paper:manage", 50));
        menus.add(menu("文件与文本", "tools/file", "course:read", 52));
        menus.add(menu("主观题阅卷", "exam/grading", "exam:manage", 55));
        menus.add(menu("考试管理", "exam", "exam:manage", 60));
        menus.add(menu("我的考试", "exam/take", "exam:student", 70));
        menus.add(menu("成绩分析", "exam/stats", "exam:analytics", 80));
        menus.add(menu("错题本", "wrongbook", "wrongbook:read", 90));

        for (SysMenu m : menus) {
            sysMenuMapper.insert(m);
        }

        List<SysMenu> all = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>());
        for (SysMenu m : all) {
            link(RoleEnum.ADMIN.name(), m.getId());
        }
        for (SysMenu m : all) {
            if (menuVisibleForRole(m, TEACHER_PERMS)) {
                link(RoleEnum.TEACHER.name(), m.getId());
            }
        }
        for (SysMenu m : all) {
            if (menuVisibleForRole(m, STUDENT_PERMS)) {
                link(RoleEnum.STUDENT.name(), m.getId());
            }
        }
    }

    private boolean menuVisibleForRole(SysMenu m, Set<String> granted) {
        String p = m.getPerms();
        if (p == null || p.isBlank()) {
            return false;
        }
        for (String part : p.split(",")) {
            String t = part.trim();
            if (!t.isEmpty() && granted.contains(t)) {
                return true;
            }
        }
        return false;
    }

    private SysMenu menu(String name, String path, String perms, int sortOrder) {
        SysMenu m = new SysMenu();
        m.setParentId(null);
        m.setName(name);
        m.setPath(path);
        m.setIcon(null);
        m.setSortOrder(sortOrder);
        m.setPerms(perms);
        m.setCreateTime(LocalDateTime.now());
        return m;
    }

    private void link(String role, Long menuId) {
        SysRoleMenu rm = new SysRoleMenu();
        rm.setRole(role);
        rm.setMenuId(menuId);
        sysRoleMenuMapper.insert(rm);
    }
}
