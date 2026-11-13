package com.xingguang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单。金额字段单位均为「分」。
 * 恒等式：payAmount = itemAmount + packFee + deliveryFee - discountAmount，且 payAmount >= 1（SRS 9.8 保底）。
 */
@Data
@TableName("orders")
public class Orders {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;
    private Long userId;
    private Long shopId;
    private String shopName;
    private String shopHeaderImg;

    /** 骑手 id（派单/接单归属），自取单为空 */
    private Long riderId;

    /** DELIVERY 平台配送 / PICKUP 到店自取 */
    private String fulfillType;
    /** 订单状态，见 OrderStatus */
    private String status;

    /** 商品小计（分） */
    private Integer itemAmount;
    /** 打包费（分） */
    private Integer packFee;
    /** 配送费（分），自取单恒为 0 */
    private Integer deliveryFee;
    /** 优惠合计（分） */
    private Integer discountAmount;
    /** 实付金额（分） */
    private Integer payAmount;

    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;

    /** 收货地址经纬度（地理编码缓存，用于配送地图打点） */
    private BigDecimal receiverLat;
    private BigDecimal receiverLng;

    /** 取餐码（仅自取单） */
    private String pickupCode;
    private String remark;
    private Integer promiseMinutes;
    private String cancelReason;

    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime finishedAt;

    private Integer deleted;
}
