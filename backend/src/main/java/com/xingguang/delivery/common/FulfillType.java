package com.xingguang.delivery.common;

/**
 * 履约方式（SRS 1.2 双履约）：平台配送 / 到店自取。
 * 自取单不生成配送单、不派单、不计配送费。
 */
public enum FulfillType {

    DELIVERY("平台配送"),
    PICKUP("到店自取");

    private final String label;

    FulfillType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static FulfillType of(String code) {
        if (code == null) {
            return DELIVERY;
        }
        for (FulfillType t : values()) {
            if (t.name().equalsIgnoreCase(code)) {
                return t;
            }
        }
        throw BizException.order("未知的履约方式：" + code);
    }
}
