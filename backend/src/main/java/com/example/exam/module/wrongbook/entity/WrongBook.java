package com.example.exam.module.wrongbook.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wrong_book")
public class WrongBook {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long courseId;

    private Long questionId;

    private Integer wrongCount;

    private LocalDateTime lastWrongAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
