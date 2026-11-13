package com.xingguang.delivery.pay;

import com.xingguang.delivery.common.BizException;
import com.xingguang.delivery.common.PayChannel;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** 支付渠道注册表：按渠道码路由到具体实现。 */
@Component
public class PaymentChannelRegistry {

    private final Map<PayChannel, PaymentChannel> channels = new EnumMap<>(PayChannel.class);

    public PaymentChannelRegistry(List<PaymentChannel> list) {
        for (PaymentChannel c : list) {
            channels.put(c.code(), c);
        }
    }

    public PaymentChannel get(PayChannel code) {
        PaymentChannel channel = channels.get(code);
        if (channel == null) {
            throw BizException.pay("支付渠道未实现：" + code);
        }
        return channel;
    }

    public List<PaymentChannel> all() {
        return List.copyOf(channels.values());
    }
}
