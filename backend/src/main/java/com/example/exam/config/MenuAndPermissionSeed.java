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

    /**
     * 已有库不会重跑 seed，但浏览/管理若曾共用 path 会导致侧栏两项同时高亮；启动时按 perms 纠正为唯一 path。
     */
    @Order(101)
    @EventListener(ApplicationReadyEvent.class)
    public void repairMenuPathsForSidebar() {
        repairPathIfWrong("course:read", "course/browse");
        repairPathIfWrong("course:manage", "course/manage");
        repairPathIfWrong("knowledge:read", "knowledge/browse");
        repairPathIfWrong("knowledge:manage", "knowledge/manage");
        repairPathIfWrong("question:read", "question/browse");
        repairPathIfWrong("question:manage", "question/manage");
        repairPathIfWrong("paper:read", "paper/browse");
        repairPathIfWrong("paper:manage", "paper/manage");
    }

    private void repairPathIfWrong(String perms, String expectedPath) {
        SysMenu m = sysMenuMapper.selectOne(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getPerms, perms));
        if (m == null || expectedPath.equals(m.getPath())) {
            return;
        }
        m.setPath(expectedPath);
        sysMenuMapper.updateById(m);
    }

    @Order(100)
    @EventListener(ApplicationReadyEvent.class)
    public void seedMenusAndRoleMenus() {
        long n = sysMenuMapper.selectCount(new LambdaQueryWrapper<SysMenu>());
        if (n > 0) {
            return;
        }
        List<SysMenu> menus = new ArrayList<>();
        menus.add(menu("用户管理", "system/user", "system:user:manage"));
        // 浏览/管理成对菜单须使用不同 path，否则 el-menu 的 index 重复会导致同时高亮
        menus.add(menu("课程浏览", "course/browse", "course:read"));
        menus.add(menu("课程管理", "course/manage", "course:manage"));
        menus.add(menu("知识点管理", "knowledge/manage", "knowledge:manage"));
        menus.add(menu("知识点浏览", "knowledge/browse", "knowledge:read"));
        menus.add(menu("题库管理", "question/manage", "question:manage"));
        menus.add(menu("题库浏览", "question/browse", "question:read"));
        menus.add(menu("试卷管理", "paper/manage", "paper:manage"));
        menus.add(menu("试卷浏览", "paper/browse", "paper:read"));
        menus.add(menu("考试管理", "exam", "exam:manage"));
        menus.add(menu("学生考试", "exam/take", "exam:student"));
        menus.add(menu("成绩统计", "exam/stats", "exam:analytics"));
        menus.add(menu("错题本", "wrongbook", "wrongbook:read"));

        for (SysMenu m : menus) {
            sysMenuMapper.insert(m);
        }

        List<SysMenu> all = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>());
        for (SysMenu m : all) {
            link(RoleEnum.ADMIN.name(), m.getId());
        }
        for (SysMenu m : all) {
            String p = m.getPerms();
            if (p != null && TEACHER_PERMS.contains(p)) {
                link(RoleEnum.TEACHER.name(), m.getId());
            }
        }
        for (SysMenu m : all) {
            String p = m.getPerms();
            if (p != null && STUDENT_PERMS.contains(p)) {
                link(RoleEnum.STUDENT.name(), m.getId());
            }
        }
    }

    private SysMenu menu(String name, String path, String perms) {
        SysMenu m = new SysMenu();
        m.setParentId(null);
        m.setName(name);
        m.setPath(path);
        m.setIcon(null);
        m.setSortOrder(0);
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
