package com.xingguang.delivery.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 结算页试算结果（配送 / 自取互斥切换后重新试算，不创建订单）。 */
@Data
public class OrderPreviewVO {

    private ShopVO shop;

    /** DELIVERY / PICKUP */
    private String fulfillType;
    private String fulfillTypeLabel;

    /** 配送单：收货地址；自取单：null（前端改为展示门店信息） */
    private AddressVO address;

    /** 参与结算的商品行 */
    private List<Line> items = new ArrayList<>();

    /** 金额明细（服务端唯一计算口径） */
    private AmountVO amount;

    /** 预计送达（配送）/ 预计备餐完成（自取）分钟数 */
    private Integer promiseMinutes;
    private String promiseText;

    /** 自取单提示：备餐完成后凭取餐码到店取餐 */
    private String pickupTip;

    /** 门店可选履约方式（用于前端切换按钮的可用性） */
    private Boolean deliverySupported = Boolean.TRUE;
    private Boolean pickupSupported = Boolean.FALSE;

    @Data
    public static class Line {
        private Long cartItemId;
        private Long dishId;
        private String dishName;
        private String dishImage;
        private String specText;
        private Integer unitPrice;
        private Integer quantity;
        private Integer amount;
    }
}
