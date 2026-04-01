package com.example.exam.module.system.service;

import com.example.exam.module.system.dto.MenuTreeVO;

import java.util.List;

public interface SysMenuService {

    List<MenuTreeVO> treeForCurrentRole();

    /** 当前角色在菜单上绑定的权限标识（与接口 hasAuthority 对齐） */
    List<String> listGrantedPermsForRole(String role);
}
