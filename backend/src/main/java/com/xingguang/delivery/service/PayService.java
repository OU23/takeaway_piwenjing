package com.xingguang.delivery.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xingguang.delivery.common.BizException;
import com.xingguang.delivery.common.OrderStatus;
import com.xingguang.delivery.common.PayChannel;
import com.xingguang.delivery.config.DeliveryProperties;
import com.xingguang.delivery.dto.PayRequest;
import com.xingguang.delivery.entity.Orders;
import com.xingguang.delivery.entity.Payment;
import com.xingguang.delivery.mapper.PaymentMapper;
import com.xingguang.delivery.pay.PaymentChannel;
import com.xingguang.delivery.pay.PaymentChannelRegistry;
import com.xingguang.delivery.vo.PayChannelVO;
import com.xingguang.delivery.vo.PrepayVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 支付服务：渠道列表 → 发起支付 → 回调确认（幂等）。
 *
 * <p>三条通道（SRS 1.5）：支付宝沙箱【保留】、微信模拟【替换】、余额【保留】。
 * 支付回调必须幂等（SRS 9.2）：同一 payNo 重复回调只结算一次。
 */
@Service
public class PayService {

    private static final Logger log = LoggerFactory.getLogger(PayService.class);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PaymentMapper paymentMapper;
    private final PaymentChannelRegistry registry;
    private final OrderService orderService;
    private final DeliveryProperties props;

    /** 渠道回调验签密钥（S1 修复：回调必须验签，防止伪造"支付成功"） */
    @org.springframework.beans.factory.annotation.Value("${xingguang.pay.notify-secret:xg-dev-pay-notify-secret}")
    private String notifySecret;

    public PayService(PaymentMapper paymentMapper, PaymentChannelRegistry registry,
                      OrderService orderService, DeliveryProperties props) {
        this.paymentMapper = paymentMapper;
        this.registry = registry;
        this.orderService = orderService;
        this.props = props;
    }

    /** 可用支付渠道（含模拟标识，前端据此提示"模拟支付"） */
    public List<PayChannelVO> channels() {
        List<PayChannelVO> list = new ArrayList<>();
        for (PaymentChannel c : registry.all()) {
            PayChannelVO vo = new PayChannelVO();
            vo.setCode(c.code().name());
            vo.setLabel(c.code().getLabel());
            vo.setMock(c.mock());
            vo.setRemark(c.remark());
            vo.setAvailable(c.available());
            list.add(vo);
        }
        return list;
    }

    /** 发起支付 */
    @Transactional(rollbackFor = Exception.class)
    public PrepayVO prepay(long userId, PayRequest req) {
        Orders order = orderService.requireOrder(req.getOrderNo());
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BizException(10003, "无权支付他人订单");
        }
        if (!OrderStatus.UNPAID.name().equals(order.getStatus())) {
            throw BizException.pay("订单当前状态为「" + OrderStatus.labelOf(order.getStatus()) + "」，无需支付");
        }

        // 已存在支付单：成功的直接返回，待支付的复用
        Payment payment = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery()
                .eq(Payment::getOrderId, order.getId())
                .orderByDesc(Payment::getCreatedAt)
                .last("limit 1"));
        if (payment != null && "SUCCESS".equals(payment.getStatus())) {
            throw BizException.pay("订单已支付，请勿重复支付");
        }
        if (payment == null) {
            payment = new Payment();
            payment.setPayNo(genPayNo());
            payment.setOrderId(order.getId());
            payment.setOrderNo(order.getOrderNo());
            payment.setChannel(req.getChannel());
            payment.setAmount(order.getPayAmount());
            payment.setStatus("WAIT");
            payment.setCreatedAt(LocalDateTime.now());
            paymentMapper.insert(payment);
        } else {
            payment.setChannel(req.getChannel());
            payment.setAmount(order.getPayAmount());
            paymentMapper.updateById(payment);
        }

        PaymentChannel channel = registry.get(PayChannel.of(req.getChannel()));
        if (!channel.available()) {
            throw BizException.pay("渠道「" + channel.code().getLabel() + "」当前不可用：" + channel.remark());
        }
        PrepayVO vo = channel.prepay(payment, order);

        if ("SUCCESS".equals(vo.getStatus())) {
            // 余额支付为同步通道，prepay 阶段即完成扣款
            settle(payment.getPayNo(), channel.genTradeNo(payment));
            vo.setStatus("SUCCESS");
        } else if (channel.mock() && props.isMockPayEnabled()) {
            // 模拟通道：由服务端在本事务内完成结算（等价于渠道服务器回调），
            // 前端不再调用 /pay/notify —— 支付成功与否只由服务端决定（S1 修复）
            settle(payment.getPayNo(), channel.genTradeNo(payment));
            vo.setStatus("SUCCESS");
        }
        return vo;
    }

    /**
     * 渠道异步通知（真实渠道服务器专用入口）。
     *
     * <p><b>S1 修复</b>：本接口不再接受无凭证调用——
     * 必须携带 {@code X-Pay-Sign = HMAC-SHA256(payNo|amount|tradeNo, 回调密钥)}，
     * 且报文金额必须与支付单一致，否则拒绝结算。
     * 幂等：同一 payNo 重复通知只结算一次（SRS 9.2 / 9.6）。
     */
    @Transactional(rollbackFor = Exception.class)
    public PrepayVO notify(String payNo, String tradeNo, Long amount, String sign) {
        Payment payment = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery()
                .eq(Payment::getPayNo, payNo));
        if (payment == null) {
            throw BizException.pay("支付单不存在：" + payNo);
        }
        verifyNotifySign(payment, tradeNo, amount, sign);
        if ("SUCCESS".equals(payment.getStatus())) {
            log.info("[支付回调幂等] payNo={} 已结算，直接返回", payNo);
        } else {
            settle(payNo, tradeNo);
        }
        Payment latest = paymentMapper.selectById(payment.getId());
        Orders order = orderService.requireOrder(latest.getOrderNo());
        PaymentChannel channel = registry.get(PayChannel.of(latest.getChannel()));
        PrepayVO vo = new PrepayVO();
        vo.setPayNo(latest.getPayNo());
        vo.setOrderNo(latest.getOrderNo());
        vo.setChannel(latest.getChannel());
        vo.setChannelLabel(channel.code().getLabel());
        vo.setAmount(latest.getAmount());
        vo.setMock(channel.mock());
        vo.setStatus(latest.getStatus());
        return vo;
    }

    /** 查询订单的支付单 */
    public Payment byOrder(long userId, String orderNo) {
        Orders order = orderService.requireOrder(orderNo);
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BizException(10003, "无权查看他人支付单");
        }
        return paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery()
                .eq(Payment::getOrderId, order.getId())
                .orderByDesc(Payment::getCreatedAt)
                .last("limit 1"));
    }

    // ------------------------------------------------------------------

    /** 渠道回调验签：签名不符或金额不符一律拒绝（恒定时间比对） */
    private void verifyNotifySign(Payment payment, String tradeNo, Long amount, String sign) {
        if (amount == null) {
            throw new BizException(10001, "回调缺少 amount 参数");
        }
        if (!Objects.equals(amount.longValue(), payment.getAmount().longValue())) {
            log.warn("[支付回调拒收] payNo={} 金额不一致：报文 {} 分 / 支付单 {} 分",
                    payment.getPayNo(), amount, payment.getAmount());
            throw new BizException(403, "回调金额与支付单不一致");
        }
        String expected = hmacSha256Hex(payment.getPayNo() + "|" + amount + "|" + (tradeNo == null ? "" : tradeNo));
        if (sign == null
                || !java.security.MessageDigest.isEqual(
                expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                sign.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            log.warn("[支付回调拒收] payNo={} 验签失败", payment.getPayNo());
            throw new BizException(403, "回调验签失败");
        }
    }

    private String hmacSha256Hex(String data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(
                    notifySecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("回调验签计算失败", e);
        }
    }

    private void settle(String payNo, String tradeNo) {
        Payment payment = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery()
                .eq(Payment::getPayNo, payNo));
        if (payment == null) {
            throw BizException.pay("支付单不存在：" + payNo);
        }
        if ("SUCCESS".equals(payment.getStatus())) {
            return;
        }
        PaymentChannel channel = registry.get(PayChannel.of(payment.getChannel()));

        payment.setStatus("SUCCESS");
        payment.setTradeNo(tradeNo == null || tradeNo.isBlank() ? channel.genTradeNo(payment) : tradeNo);
        payment.setPaidAt(LocalDateTime.now());
        paymentMapper.updateById(payment);

        Orders order = orderService.requireOrder(payment.getOrderNo());
        channel.onPaySuccess(payment, order);
        orderService.onPaid(order, payment);
        log.info("[支付成功] payNo={} orderNo={} channel={} amount={}分",
                payment.getPayNo(), payment.getOrderNo(), payment.getChannel(), payment.getAmount());
    }

    private String genPayNo() {
        for (int i = 0; i < 5; i++) {
            String no = "PAY" + LocalDateTime.now().format(NO_FMT)
                    + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
            if (paymentMapper.selectCount(Wrappers.<Payment>lambdaQuery()
                    .eq(Payment::getPayNo, no)) == 0) {
                return no;
            }
        }
        throw BizException.pay("支付单号生成失败，请重试");
    }

    /** 供启动自检展示（SRS 9.3：生产环境必须关闭模拟开关） */
    public boolean mockPayEnabled() {
        return props.isMockPayEnabled();
    }
}
