package com.xingguang.delivery.vo;

import lombok.Data;

/** 用户信息（手机号脱敏，SRS 9.11.6）。 */
@Data
public class UserVO {

    private Long id;
    /** 脱敏手机号：138****1111 */
    private String phoneMasked;
    private String nickname;
    private String avatar;

    /** 钱包余额（分） */
    private Integer balance;

    private String memberLevel;
    private String memberLevelLabel;
    private Integer growth;
    /** 距下一等级所需成长值 */
    private Integer growthToNext;
    private String nextLevelLabel;
}
