package com.example.exam.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.exam.common.api.ErrorCode;
import com.example.exam.common.enums.RoleEnum;
import com.example.exam.common.exception.BizException;
import com.example.exam.common.security.SecurityHelper;
import com.example.exam.module.system.dto.MenuTreeVO;
import com.example.exam.module.system.entity.SysMenu;
import com.example.exam.module.system.entity.SysRoleMenu;
import com.example.exam.module.system.entity.User;
import com.example.exam.module.system.mapper.SysMenuMapper;
import com.example.exam.module.system.mapper.SysRoleMenuMapper;
import com.example.exam.module.system.service.SysMenuService;
import com.example.exam.module.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl implements SysMenuService {

    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final UserService userService;

    @Override
    public List<MenuTreeVO> treeForCurrentRole() {
        User u = userService.findByUsername(SecurityHelper.requireUsername());
        if (u == null) {
            return List.of();
        }
        String role = u.getRole();
        List<Long> menuIds = sysRoleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRole, role)).stream()
                .map(SysRoleMenu::getMenuId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (menuIds.isEmpty()) {
            return List.of();
        }
        List<SysMenu> menus = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getId, menuIds)
                .orderByAsc(SysMenu::getSortOrder)
                .orderByAsc(SysMenu::getId));
        Map<Long, MenuTreeVO> nodes = new HashMap<>();
        for (SysMenu m : menus) {
            MenuTreeVO vo = new MenuTreeVO();
            vo.setId(m.getId());
            vo.setParentId(m.getParentId());
            vo.setName(m.getName());
            vo.setPath(m.getPath());
            vo.setIcon(m.getIcon());
            vo.setSortOrder(m.getSortOrder());
            vo.setPerms(m.getPerms());
            nodes.put(m.getId(), vo);
        }
        List<MenuTreeVO> roots = new ArrayList<>();
        for (MenuTreeVO vo : nodes.values()) {
            Long pid = vo.getParentId();
            if (pid == null || !nodes.containsKey(pid)) {
                roots.add(vo);
            } else {
                nodes.get(pid).getChildren().add(vo);
            }
        }
        Comparator<MenuTreeVO> cmp = Comparator
                .comparing((MenuTreeVO x) -> x.getSortOrder() == null ? 0 : x.getSortOrder())
                .thenComparing(MenuTreeVO::getId);
        sortTree(roots, cmp);
        return roots.stream().sorted(cmp).collect(Collectors.toList());
    }

    @Override
    public List<String> listGrantedPermsForRole(String role) {
        if (RoleEnum.ADMIN.name().equals(role)) {
            return sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                            .isNotNull(SysMenu::getPerms)
                            .ne(SysMenu::getPerms, ""))
                    .stream()
                    .map(SysMenu::getPerms)
                    .flatMap(p -> Arrays.stream(p.split(",")))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .toList();
        }
        List<Long> menuIds = sysRoleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRole, role)).stream()
                .map(SysRoleMenu::getMenuId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (menuIds.isEmpty()) {
            return List.of();
        }
        return sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                        .in(SysMenu::getId, menuIds)
                        .isNotNull(SysMenu::getPerms)
                        .ne(SysMenu::getPerms, ""))
                .stream()
                .map(SysMenu::getPerms)
                .flatMap(p -> Arrays.stream(p.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    @Override
    public List<SysMenu> listAllMenus() {
        return sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSortOrder)
                .orderByAsc(SysMenu::getId));
    }

    @Override
    public void deleteMenu(Long id) {
        SysMenu root = sysMenuMapper.selectById(id);
        if (root == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "菜单不存在");
        }

        List<SysMenu> allMenus = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .select(SysMenu::getId, SysMenu::getParentId));
        Map<Long, List<Long>> childMap = new HashMap<>();
        for (SysMenu m : allMenus) {
            Long pid = m.getParentId();
            if (pid == null) {
                continue;
            }
            childMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(m.getId());
        }

        Set<Long> idsToDelete = new HashSet<>();
        ArrayList<Long> stack = new ArrayList<>();
        stack.add(id);
        while (!stack.isEmpty()) {
            Long cur = stack.remove(stack.size() - 1);
            if (!idsToDelete.add(cur)) {
                continue;
            }
            List<Long> children = childMap.get(cur);
            if (children != null && !children.isEmpty()) {
                stack.addAll(children);
            }
        }

        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                .in(SysRoleMenu::getMenuId, idsToDelete));
        sysMenuMapper.delete(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getId, idsToDelete));
    }

    private void sortTree(List<MenuTreeVO> list, Comparator<MenuTreeVO> cmp) {
        list.sort(cmp);
        for (MenuTreeVO n : list) {
            if (n.getChildren() != null && !n.getChildren().isEmpty()) {
                sortTree(n.getChildren(), cmp);
            }
        }
    }
}
