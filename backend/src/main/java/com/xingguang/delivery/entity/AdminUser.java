package com.xingguang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 运营后台账号（RBAC）。 */
@Data
@TableName("admin_user")
public class AdminUser {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String username;
    private String password;
    private String name;
    /** SUPER_ADMIN 超级管理员 / OPERATOR 运营 / CS 客服 */
    private String role;
    private String phone;
    /** 1 启用 / 0 停用 */
    private Integer status;
    private LocalDateTime lastLogin;
    private Integer deleted;
    private LocalDateTime createdAt;
}
