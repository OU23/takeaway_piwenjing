package com.xingguang.delivery.common;

/**
 * 支付渠道（SRS 1.5）。
 * <p>微信支付需企业商户号 →【替换】为模拟通道，但保留统一 PaymentChannel 抽象与实现代码，
 * 运行时切换即接入真实通道。
 */
public enum PayChannel {

    ALIPAY_SANDBOX("支付宝(沙箱)", "支付宝沙箱（个人可注册，SRS【保留】）"),
    WECHAT_MOCK("微信(模拟)", "需企业商户号 →【替换】为模拟支付通道"),
    BALANCE("余额支付", "平台自建钱包，SRS【保留】");

    private final String label;
    private final String remark;

    PayChannel(String label, String remark) {
        this.label = label;
        this.remark = remark;
    }

    public String getLabel() {
        return label;
    }

    public String getRemark() {
        return remark;
    }

    public static PayChannel of(String code) {
        if (code == null) {
            return null;
        }
        for (PayChannel c : values()) {
            if (c.name().equalsIgnoreCase(code)) {
                return c;
            }
        }
        throw BizException.pay("不支持的支付渠道：" + code);
    }
}
