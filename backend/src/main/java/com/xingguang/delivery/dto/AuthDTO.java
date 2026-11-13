package com.xingguang.delivery.dto;

import lombok.Data;

/** 认证相关入参。 */
public class AuthDTO {

    /** 注册：用户名 + 邮箱 + 手机号（需短信验证）+ 密码 */
    @Data
    public static class Register {
        private String username;
        private String email;
        private String phone;
        /** 短信验证码，由 /auth/sms/code 下发 */
        private String code;
        private String password;
    }

    /** 登录：account 可填用户名或邮箱 */
    @Data
    public static class Login {
        private String account;
        private String password;
    }

    /** 发送短信验证码 */
    @Data
    public static class SmsSend {
        private String phone;
    }

    /** 手机验证码登录 */
    @Data
    public static class SmsLogin {
        private String phone;
        private String code;
    }
}
