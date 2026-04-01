package com.example.exam.module.system.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MenuTreeVO {

    private Long id;

    private Long parentId;

    private String name;

    private String path;

    private String icon;

    private Integer sortOrder;

    private String perms;

    private List<MenuTreeVO> children = new ArrayList<>();
}
