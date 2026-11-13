package com.xingguang.delivery.dto;

import lombok.Data;

/** 运营后台登录入参（原型：MD5 比对，真实应替换为 JWT 签发）。 */
@Data
public class AdminLoginDTO {
    private String username;
    private String password;
}
