package com.xingguang.delivery.vo;

import lombok.Data;

/**
 * 金额明细（单位：分）。
 *
 * <p>恒等式（SRS 9.7 / 9.8）：
 * <pre>
 *   payAmount = itemAmount + packFee + deliveryFee - discountAmount
 *   discountAmount = fullReduceAmount + pickupDiscountAmount
 *   payAmount >= 1（保底，不允许 0 元或负数成交）
 * </pre>
 * 金额一律由服务端最终计算一次，前端结果仅作展示。
 */
@Data
public class AmountVO {

    /** 商品小计 */
    private Integer itemAmount = 0;
    /** 打包费 */
    private Integer packFee = 0;
    /** 配送费（自取单为 0） */
    private Integer deliveryFee = 0;
    /** 原配送费（自取单用于「划线展示 + 自取免配送费」标签） */
    private Integer deliveryFeeOrigin = 0;
    /** 满减金额 */
    private Integer fullReduceAmount = 0;
    /** 自取立减金额 */
    private Integer pickupDiscountAmount = 0;
    /** 优惠合计 */
    private Integer discountAmount = 0;
    /** 实付金额 */
    private Integer payAmount = 0;

    /** 起送价（分） */
    private Integer minPrice = 0;
    /** 是否达到起送价 */
    private Boolean reachMin = Boolean.TRUE;
    /** 距起送价差额（分），未达标时 > 0 */
    private Integer minPriceGap = 0;

    /** 命中/未命中的满减规则描述，如「满30减8」 */
    private String fullReduceRule;
    /** 下一档满减提示，如「再买 ¥6.00 可减 ¥12.00」 */
    private String nextFullReduceHint;

    /** 商品总件数 */
    private Integer totalQuantity = 0;
}
