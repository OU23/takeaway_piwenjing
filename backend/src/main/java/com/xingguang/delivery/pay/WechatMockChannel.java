package com.xingguang.delivery.pay;

import com.xingguang.delivery.common.BizException;
import com.xingguang.delivery.common.PayChannel;
import com.xingguang.delivery.config.DeliveryProperties;
import com.xingguang.delivery.entity.Orders;
import com.xingguang.delivery.entity.Payment;
import com.xingguang.delivery.vo.PrepayVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 微信支付渠道（SRS 1.5【替换】）。
 *
 * <p>微信支付 V3 需企业/个体户商户号，个人无法申请。因此保留完整实现位与调用形态，
 * 运行时切换为「模拟支付通道」——由本地流水登记 + 一键支付成功完成闭环。
 * 后续拿到商户号后，只需替换 {@link #prepay} 与 {@link #onPaySuccess} 的通道调用即可。
 */
@Component
public class WechatMockChannel implements PaymentChannel {

    private static final Logger log = LoggerFactory.getLogger(WechatMockChannel.class);

    private final DeliveryProperties props;

    public WechatMockChannel(DeliveryProperties props) {
        this.props = props;
    }

    @Override
    public PayChannel code() {
        return PayChannel.WECHAT_MOCK;
    }

    @Override
    public boolean available() {
        return props.isMockPayEnabled();
    }

    @Override
    public boolean mock() {
        return true;
    }

    @Override
    public String remark() {
        return "需企业商户号 → 模拟支付通道（一键支付成功，生产环境必须关闭，SRS 9.3）";
    }

    @Override
    public PrepayVO prepay(Payment payment, Orders order) {
        if (!props.isMockPayEnabled()) {
            throw BizException.pay("模拟支付通道已关闭，请配置真实微信商户号");
        }
        PrepayVO vo = new PrepayVO();
        vo.setPayNo(payment.getPayNo());
        vo.setOrderNo(order.getOrderNo());
        vo.setChannel(code().name());
        vo.setChannelLabel(code().getLabel());
        vo.setAmount(payment.getAmount());
        vo.setMock(true);
        vo.setStatus(payment.getStatus());
        vo.setPayUrl(null);
        log.info("[微信模拟通道] 受理支付 payNo={} amount={}分", payment.getPayNo(), payment.getAmount());
        return vo;
    }

    @Override
    public void onPaySuccess(Payment payment, Orders order) {
        log.info("[微信模拟通道] 支付成功 payNo={} tradeNo={}", payment.getPayNo(), payment.getTradeNo());
    }
}
