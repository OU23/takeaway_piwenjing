package com.xingguang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 门店。含双履约相关配置：起送价 / 配送费 / 打包费 / 到店自取与自取立减。
 * 所有金额字段单位均为「分」。
 */
@Data
@TableName("shop")
public class Shop {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long merchantId;
    private String name;
    private String headerImg;
    private String logo;

    private BigDecimal rating;
    private Integer monthSales;

    private String address;
    private BigDecimal lat;
    private BigDecimal lng;
    private String notice;

    private String openTime;
    private String closeTime;

    /** OPEN 营业 / CLOSED 打烊 / REST 休息 */
    private String status;

    /** 起送价（分） */
    private Integer minPrice;
    /** 配送费（分） */
    private Integer deliveryFee;
    /** 打包费（分/件） */
    private Integer packFee;
    /** 预计送达（分钟） */
    private Integer etaMinutes;

    /** 是否支持到店自取 */
    private Integer pickupEnabled;
    /** 自取立减（分） */
    private Integer pickupDiscount;

    private BigDecimal distanceKm;
    /** 活动标签，逗号分隔 */
    private String tags;

    private Integer deleted;
    private LocalDateTime createdAt;
}
