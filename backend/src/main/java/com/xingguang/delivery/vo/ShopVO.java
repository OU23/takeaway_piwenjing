package com.xingguang.delivery.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 门店卡片 / 门店概要（首页附近好店、搜索结果）。 */
@Data
public class ShopVO {

    private Long id;
    private Long merchantId;
    private String name;
    private String logo;
    private String headerImg;

    private BigDecimal rating;
    private Integer monthSales;
    private String address;
    private String notice;

    private String openTime;
    private String closeTime;
    /** OPEN 营业 / CLOSED 打烊 */
    private String status;
    private String statusLabel;

    /** 起送价（分） */
    private Integer minPrice;
    /** 配送费（分） */
    private Integer deliveryFee;
    /** 预计送达（分钟） */
    private Integer etaMinutes;

    private Boolean pickupEnabled;
    /** 自取立减（分） */
    private Integer pickupDiscount;

    private BigDecimal distanceKm;

    /** 门店经纬度（用于地图打点） */
    private BigDecimal lat;
    private BigDecimal lng;

    /** 活动标签 */
    private List<String> tags = new ArrayList<>();
    /** 满减规则描述，如「满 ¥30 减 ¥8」 */
    private List<String> promotions = new ArrayList<>();
}
