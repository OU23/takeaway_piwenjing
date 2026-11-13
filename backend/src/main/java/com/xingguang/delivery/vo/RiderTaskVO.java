package com.xingguang.delivery.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 骑手任务视图（接单大厅 / 我的任务 / 任务详情共用）。 */
@Data
public class RiderTaskVO {

    private String orderNo;
    private Long shopId;
    private String shopName;
    private String shopHeaderImg;
    /** 门店经纬度（地图打点用） */
    private BigDecimal shopLat;
    private BigDecimal shopLng;

    private String fulfillType;
    private String fulfillTypeLabel;
    private String status;
    private String statusLabel;
    private String statusGroup;

    private String receiverName;
    /** 脱敏手机号（骑手查看一律脱敏，SRS 9.11.6） */
    private String receiverPhone;
    private String receiverAddress;

    /** 实付金额（分） */
    private Integer payAmount;
    /** 商品件数 */
    private Integer itemCount;
    /** 演示距离（km） */
    private BigDecimal distanceKm;
    private Integer etaMinutes;
    /** 承诺送达时间文案 */
    private String promiseText;

    private LocalDateTime createdAt;

    /** 是否可「取货」（WAIT_PICKUP） */
    private Boolean canPickup = Boolean.FALSE;
    /** 是否可「送达」（DELIVERING） */
    private Boolean canDeliver = Boolean.FALSE;
}
