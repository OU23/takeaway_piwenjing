package com.xingguang.delivery.pay;

import com.xingguang.delivery.common.PayChannel;
import com.xingguang.delivery.entity.Orders;
import com.xingguang.delivery.entity.Payment;
import com.xingguang.delivery.vo.PrepayVO;

/**
 * 支付渠道抽象（SRS 1.5 核心约定）。
 *
 * <p>微信支付、支付宝沙箱、余额支付全部实现同一接口，业务代码不感知具体通道；
 * 运行时切换实现即可在"真实通道 / 模拟通道"之间无缝替换，接口形态与业务逻辑保持一致。
 */
public interface PaymentChannel {

    /** 渠道码 */
    PayChannel code();

    /** 当前是否可用（未配置密钥的渠道在列表中置灰） */
    boolean available();

    /** 是否为模拟通道：前端据此走"一键支付成功"演示路径（SRS 9.3 生产必须关闭） */
    boolean mock();

    /** 渠道说明（展示用） */
    String remark();

    /** 发起支付：落支付单并返回调起参数 */
    PrepayVO prepay(Payment payment, Orders order);

    /** 渠道回调确认支付成功后的事务内副作用（如余额扣减、记账） */
    void onPaySuccess(Payment payment, Orders order);

    /**
     * 订单取消时的资金退回（事务内，由 OrderService.cancel 触发）。
     *
     * <p>默认空实现：模拟通道与沙箱通道没有真实资金流，退单只需把支付单标记为 REFUNDED；
     * 余额通道必须覆写，否则用户付了款又取消，钱会凭空消失。
     *
     * <p>实现方必须保证幂等：同一支付单重复退款只能退一次。
     */
    default void onRefund(Payment payment, Orders order) {
        // 无真实资金流，无需回退
    }

    /** 生成渠道交易号（真实通道由渠道返回；此处给出统一口径，便于本地流水与对账） */
    default String genTradeNo(Payment payment) {
        return code().name() + System.currentTimeMillis()
                + String.format("%03d", (int) (Math.random() * 1000));
    }
}
