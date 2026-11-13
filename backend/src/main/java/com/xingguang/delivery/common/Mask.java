package com.xingguang.delivery.common;

/**
 * 脱敏工具（SRS 9.11.6）：手机号 138****8888。
 * 后台 / 客服 / 骑手 / 商家查看手机号一律脱敏，完整值需二次授权并记审计日志。
 */
public final class Mask {

    private Mask() {
    }

    public static String phone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    public static String idCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }
}
