package com.xingguang.delivery.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 订单视图（列表 / 详情共用）。 */
@Data
public class OrderVO {

    private Long id;
    private String orderNo;

    private Long shopId;
    private String shopName;
    private String shopHeaderImg;
    /** 门店经纬度（详情页配送地图打点用） */
    private BigDecimal shopLat;
    private BigDecimal shopLng;

    /** DELIVERY 平台配送 / PICKUP 到店自取 */
    private String fulfillType;
    private String fulfillTypeLabel;

    private String status;
    private String statusLabel;
    /** 前端 Tab 分组：待付款 / 待接单 / 进行中 / 待评价 / 退款售后 */
    private String statusGroup;

    private Integer itemAmount;
    private Integer packFee;
    private Integer deliveryFee;
    private Integer discountAmount;
    private Integer payAmount;
    private Integer totalQuantity;

    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    /** 收货地址经纬度（配送地图打点用） */
    private BigDecimal receiverLat;
    private BigDecimal receiverLng;

    /** 取餐码（自取单） */
    private String pickupCode;
    private String remark;
    private Integer promiseMinutes;
    private String cancelReason;

    /** 承诺送达时间文案 */
    private String promiseText;

    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime finishedAt;

    /** 前端可执行的用户侧操作 */
    private List<String> actions = new ArrayList<>();
    /** 是否可催单（限频，SRS 4.4） */
    private Boolean canUrge = Boolean.FALSE;

    private List<Item> items = new ArrayList<>();
    private List<Log> timeline = new ArrayList<>();

    /** 试算/实付金额明细（含起送价校验），仅详情页返回 */
    private AmountVO amount;

    @Data
    public static class Item {
        private Long dishId;
        private String dishName;
        private String dishImage;
        private String specText;
        private Integer unitPrice;
        private Integer quantity;
        private Integer amount;
    }

    @Data
    public static class Log {
        private String fromStatus;
        private String fromLabel;
        private String toStatus;
        private String toLabel;
        private String operator;
        private String operatorLabel;
        private String remark;
        private LocalDateTime createdAt;
    }
}
