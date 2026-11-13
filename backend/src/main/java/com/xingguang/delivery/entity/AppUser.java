package com.xingguang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户（消费者）。balance 为钱包余额（分）。 */
@Data
@TableName("app_user")
public class AppUser {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 登录账号（3-20 位字母数字下划线，唯一） */
    private String username;

    /** 邮箱（唯一，可用于登录与找回密码） */
    private String email;

    /** 密码摘要（PBKDF2，格式见 support.PasswordCodec）；第三方登录用户为 null */
    private String passwordHash;

    /** 手机号；账号注册用户可为空，不再作为唯一身份 */
    private String phone;
    private String nickname;
    private String avatar;

    /** 钱包余额（分） */
    private Integer balance;

    /** NORMAL / SILVER / GOLD / PLATINUM / BLACKGOLD / BLACKDIAMOND */
    private String memberLevel;
    /** 成长值（只增不减，决定等级） */
    private Integer growth;

    private Integer deleted;
    private LocalDateTime createdAt;
}
