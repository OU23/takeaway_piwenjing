package com.xingguang.delivery.vo;

import lombok.Data;

/** 登录 / 注册 / 当前会话的统一返回体。 */
@Data
public class AuthVO {

    /** 访问令牌；{@code /auth/me} 不返回 */
    private String token;

    /** 令牌过期时刻（Unix 秒）；{@code /auth/me} 不返回 */
    private Long expiresAt;

    private User user;

    @Data
    public static class User {
        private Long id;
        private String username;
        private String email;
        private String nickname;
        private String avatar;
        /** 钱包余额（分） */
        private Integer balance;
        private String memberLevel;
        private String memberLevelLabel;
    }
}
