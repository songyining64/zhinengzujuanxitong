package com.example.exam.module.system.service;

import com.example.exam.module.system.dto.MenuTreeVO;
import com.example.exam.module.system.entity.SysMenu;

import java.util.List;

public interface SysMenuService {

    List<MenuTreeVO> treeForCurrentRole();

    /** 当前角色在菜单上绑定的权限标识（与接口 hasAuthority 对齐） */
    List<String> listGrantedPermsForRole(String role);

    /** 管理员使用：查询全部菜单（扁平） */
    List<SysMenu> listAllMenus();

    /** 管理员使用：删除菜单并清理角色绑定 */
    void deleteMenu(Long id);
}
