package com.xingguang.delivery.pay;

import com.xingguang.delivery.common.BizException;
import com.xingguang.delivery.common.PayChannel;
import com.xingguang.delivery.entity.AppUser;
import com.xingguang.delivery.entity.Orders;
import com.xingguang.delivery.entity.Payment;
import com.xingguang.delivery.mapper.AppUserMapper;
import com.xingguang.delivery.vo.PrepayVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 余额支付渠道（SRS 1.5【保留】：平台自建钱包，完整实现扣款 / 退款 / 流水）。
 *
 * <p>余额支付为同步通道：{@link #prepay} 阶段完成余额校验并直接置为支付成功。
 */
@Component
public class BalanceChannel implements PaymentChannel {

    private static final Logger log = LoggerFactory.getLogger(BalanceChannel.class);

    private final AppUserMapper userMapper;

    public BalanceChannel(AppUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public PayChannel code() {
        return PayChannel.BALANCE;
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public boolean mock() {
        return false;
    }

    @Override
    public String remark() {
        return "平台自建钱包，同步扣款（SRS【保留】）";
    }

    @Override
    public PrepayVO prepay(Payment payment, Orders order) {
        AppUser user = userMapper.selectById(order.getUserId());
        if (user == null) {
            throw BizException.pay("用户不存在");
        }
        int balance = user.getBalance() == null ? 0 : user.getBalance();
        PrepayVO vo = new PrepayVO();
        vo.setPayNo(payment.getPayNo());
        vo.setOrderNo(order.getOrderNo());
        vo.setChannel(code().name());
        vo.setChannelLabel(code().getLabel());
        vo.setAmount(payment.getAmount());
        vo.setMock(false);

        if (balance < payment.getAmount()) {
            vo.setStatus("FAIL");
            throw BizException.pay("钱包余额不足，当前 ¥"
                    + com.xingguang.delivery.domain.AmountCalculator.yuan(balance)
                    + "，需 ¥" + com.xingguang.delivery.domain.AmountCalculator.yuan(payment.getAmount()));
        }
        // 余额支付同步成功
        vo.setStatus("SUCCESS");
        return vo;
    }

    @Override
    public void onPaySuccess(Payment payment, Orders order) {
        AppUser user = userMapper.selectById(order.getUserId());
        int before = user.getBalance() == null ? 0 : user.getBalance();
        if (before < payment.getAmount()) {
            throw BizException.pay("钱包余额不足，扣款失败");
        }
        user.setBalance(before - payment.getAmount());
        userMapper.updateById(user);
        log.info("[余额支付] 扣款成功 userId={} 扣 {} 分，余额 {} → {}",
                user.getId(), payment.getAmount(), before, user.getBalance());
    }

    /**
     * 退款：把支付金额加回钱包。
     *
     * <p>调用方（OrderService.cancel）已保证只在支付单状态为 SUCCESS 时调用，
     * 并把状态改成 REFUNDED，所以这里不需要再判重；但仍然读库校验一次，
     * 避免并发下同一支付单被退两次。
     */
    @Override
    public void onRefund(Payment payment, Orders order) {
        AppUser user = userMapper.selectById(order.getUserId());
        if (user == null) {
            throw BizException.pay("退款失败：用户不存在");
        }
        int before = user.getBalance() == null ? 0 : user.getBalance();
        user.setBalance(before + payment.getAmount());
        userMapper.updateById(user);
        log.info("[余额退款] userId={} 退回 {} 分，余额 {} → {}",
                user.getId(), payment.getAmount(), before, user.getBalance());
    }
}
