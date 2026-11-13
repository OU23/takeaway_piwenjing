package com.xingguang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 支付单。一单一支付单（SRS 2.5：支付单 1:1 订单）。 */
@Data
@TableName("payment")
public class Payment {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String payNo;
    private Long orderId;
    private String orderNo;

    /** ALIPAY_SANDBOX / WECHAT_MOCK / BALANCE */
    private String channel;
    /** 金额（分） */
    private Integer amount;
    /** WAIT / SUCCESS / FAIL / CLOSED / REFUNDED */
    private String status;
    private String tradeNo;

    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    /** 退款时间：仅 REFUNDED 有值，用于对账时区分「何时退的」 */
    private LocalDateTime refundedAt;
    private Integer deleted;
}
