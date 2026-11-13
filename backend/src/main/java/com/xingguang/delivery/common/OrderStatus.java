package com.xingguang.delivery.common;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 订单状态枚举（SRS 8.9 状态机全集）。
 *
 * <p>配送单：待支付 → 待接单 → 备餐中 → 待取货 → 配送中 → 已送达 → 已完成
 * <p>自取单：待支付 → 待接单 → 备餐中 → 待取餐 → 已取餐 → 已完成
 * <p>两类订单共用同一套状态机，自取单不引入任何新状态。
 */
public enum OrderStatus {

    UNPAID("待支付", "待付款"),
    WAIT_ACCEPT("待接单", "待接单"),
    PREPARING("备餐中", "进行中"),
    WAIT_PICKUP("待取货", "进行中"),
    DELIVERING("配送中", "进行中"),
    DELIVERED("已送达", "进行中"),
    WAIT_MEAL("待取餐", "进行中"),
    PICKED("已取餐", "进行中"),
    COMPLETED("已完成", "待评价"),
    CANCELED("已取消", "退款售后"),
    REFUNDED("已退款", "退款售后"),
    PART_REFUNDED("部分退款", "退款售后"),
    EXCEPTION("异常", "进行中");

    /** 状态码 */
    private final String code;
    /** 中文标签 */
    private final String label;
    /** 归属的前端 Tab 分组 */
    private final String group;

    OrderStatus(String label, String group) {
        this.code = name();
        this.label = label;
        this.group = group;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getGroup() {
        return group;
    }

    public static OrderStatus of(String code) {
        if (code == null) {
            return null;
        }
        for (OrderStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        return null;
    }

    public static String labelOf(String code) {
        OrderStatus s = of(code);
        return s == null ? code : s.label;
    }

    /** 供前端渲染状态字典 */
    public static Map<String, String> dict() {
        Map<String, String> map = new LinkedHashMap<>();
        for (OrderStatus s : values()) {
            map.put(s.code, s.label);
        }
        return map;
    }
}
