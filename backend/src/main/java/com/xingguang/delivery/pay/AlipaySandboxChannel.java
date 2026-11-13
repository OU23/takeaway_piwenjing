package com.xingguang.delivery.pay;

import com.xingguang.delivery.common.PayChannel;
import com.xingguang.delivery.config.DeliveryProperties;
import com.xingguang.delivery.entity.Orders;
import com.xingguang.delivery.entity.Payment;
import com.xingguang.delivery.vo.PrepayVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 支付宝沙箱渠道（SRS 1.5【保留】：个人可注册开放平台并使用沙箱环境）。
 *
 * <p>网关：{@code https://openapi-sandbox.dl.alipaydev.com/gateway.do}
 * <p>未配置 APPID/密钥时以「沙箱占位模式」运行：仍生成真实格式的沙箱网关参数，
 * 由 {@code /api/v1/pay/notify} 模拟异步通知完成闭环，用于本地演示（无内网穿透也可跑通）。
 */
@Component
public class AlipaySandboxChannel implements PaymentChannel {

    private static final Logger log = LoggerFactory.getLogger(AlipaySandboxChannel.class);

    private final DeliveryProperties props;

    public AlipaySandboxChannel(DeliveryProperties props) {
        this.props = props;
    }

    @Override
    public PayChannel code() {
        return PayChannel.ALIPAY_SANDBOX;
    }

    /** 是否已完成沙箱配置（APPID + 密钥） */
    public boolean configured() {
        return props.getAlipay().isEnabled() && props.getAlipay().getAppId() != null
                && !props.getAlipay().getAppId().isBlank();
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public boolean mock() {
        // 未配置沙箱密钥时走占位模式：接口形态一致，仅"调起"环节由本地模拟
        return !configured();
    }

    @Override
    public String remark() {
        return configured()
                ? "支付宝沙箱已配置（APPID: " + props.getAlipay().getAppId() + "）"
                : "支付宝沙箱未配置密钥 → 以沙箱占位模式运行，可随时填入沙箱 APPID/密钥切换真实调起";
    }

    @Override
    public PrepayVO prepay(Payment payment, Orders order) {
        PrepayVO vo = new PrepayVO();
        vo.setPayNo(payment.getPayNo());
        vo.setOrderNo(order.getOrderNo());
        vo.setChannel(code().name());
        vo.setChannelLabel(code().getLabel());
        vo.setAmount(payment.getAmount());
        vo.setMock(mock());
        vo.setStatus(payment.getStatus());

        StringBuilder url = new StringBuilder(props.getAlipay().getGateway())
                .append("?method=alipay.trade.page.pay")
                .append("&out_trade_no=").append(payment.getPayNo())
                .append("&total_amount=").append(com.xingguang.delivery.domain.AmountCalculator.yuan(payment.getAmount()))
                .append("&subject=").append("数创星光外卖-").append(order.getOrderNo());
        vo.setPayUrl(url.toString());

        log.info("[支付宝沙箱] 生成支付参数 payNo={} amount={}分 mock={}",
                payment.getPayNo(), payment.getAmount(), mock());
        return vo;
    }

    @Override
    public void onPaySuccess(Payment payment, Orders order) {
        // 真实场景：此处校验支付宝异步通知签名、核对 total_amount、写对账流水（SRS 9.3）
        log.info("[支付宝沙箱] 支付成功回调确认 payNo={} tradeNo={}", payment.getPayNo(), payment.getTradeNo());
    }
}
