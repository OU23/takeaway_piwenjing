package com.xingguang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 骑手（SRS 1.4 骑手端 App）。 */
@Data
@TableName("rider")
public class Rider {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;
    private String phone;
    private String avatar;
    /** ONLINE 接单中 / OFFLINE 下班 */
    private String status;
    /** 评分 */
    private BigDecimal rating;
    /** 配送工具 */
    private String vehicle;
    private String idCard;
    /** 累计完成单量 */
    private Integer completedCount;
    /** 钱包余额（分），含已结算收入 */
    private Integer balance;

    private Integer deleted;
    private LocalDateTime createdAt;
}
