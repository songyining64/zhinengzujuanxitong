package com.example.exam.module.system.service;

import com.example.exam.module.system.dto.MenuTreeVO;

import java.util.List;

public interface SysMenuService {

    List<MenuTreeVO> treeForCurrentRole();
}
