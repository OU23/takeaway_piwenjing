package com.xingguang.delivery.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 骑手档案视图。 */
@Data
public class RiderProfileVO {

    private Long id;
    private String name;
    /** 脱敏手机号 */
    private String phoneMasked;
    private String avatar;
    /** ONLINE / OFFLINE */
    private String status;
    private Boolean online;
    private BigDecimal rating;
    private String vehicle;
    /** 累计完成单量 */
    private Integer completedCount;
    /** 钱包余额（分） */
    private Integer balance;
    /** 今日收入（分） */
    private Integer todayIncome;
}
