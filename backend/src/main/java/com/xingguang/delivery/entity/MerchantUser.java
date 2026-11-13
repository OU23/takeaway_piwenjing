package com.xingguang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 商家端登录账号（按门店隔离：一个账号管理一家店）。 */
@Data
@TableName("merchant_user")
public class MerchantUser {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long merchantId;
    private Long shopId;
    private String username;
    private String password;
    private String name;
    private String phone;
    /** 1 启用 / 0 停用 */
    private Integer status;
    private LocalDateTime lastLogin;
    private Integer deleted;
    private LocalDateTime createdAt;
}
