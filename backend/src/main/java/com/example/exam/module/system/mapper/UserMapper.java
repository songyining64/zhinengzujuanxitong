package com.example.exam.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.exam.module.system.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}

