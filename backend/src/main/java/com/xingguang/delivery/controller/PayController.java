package com.xingguang.delivery.controller;

import com.xingguang.delivery.common.Result;
import com.xingguang.delivery.dto.PayRequest;
import com.xingguang.delivery.entity.Payment;
import com.xingguang.delivery.service.PayService;
import com.xingguang.delivery.support.CurrentUser;
import com.xingguang.delivery.vo.PayChannelVO;
import com.xingguang.delivery.vo.PrepayVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 支付（SRS 5.2.5 / 4.8）。 */
@Tag(name = "05 支付", description = "支付宝沙箱 / 微信模拟 / 余额三条通道")
@RestController
@RequestMapping("/api/v1/pay")
public class PayController {

    private final PayService payService;
    private final CurrentUser currentUser;

    public PayController(PayService payService, CurrentUser currentUser) {
        this.payService = payService;
        this.currentUser = currentUser;
    }

    @Operation(summary = "可用支付渠道", description = "mock=true 为模拟通道，前端需展示「模拟支付」提示")
    @GetMapping("/channels")
    public Result<List<PayChannelVO>> channels() {
        return Result.ok(payService.channels());
    }

    @Operation(summary = "发起支付", description = "余额支付同步成功；模拟 / 沙箱返回待回调")
    @PostMapping("/prepay")
    public Result<PrepayVO> prepay(@Valid @RequestBody PayRequest req) {
        return Result.ok(payService.prepay(currentUser.userId(), req));
    }

    @Operation(summary = "支付回调（仅渠道服务器可调）",
            description = "幂等：同一 payNo 重复通知只结算一次。必须携带 X-Pay-Sign 验签头"
                    + "（HMAC-SHA256(payNo|amount|tradeNo, 渠道回调密钥)），且报文金额须与支付单一致；"
                    + "前端禁止调用本接口。")
    @PostMapping("/notify/{payNo}")
    public Result<PrepayVO> notify(@PathVariable String payNo,
                                   @RequestParam(required = false) String tradeNo,
                                   @RequestParam Long amount,
                                   @RequestHeader(value = "X-Pay-Sign", required = false) String sign) {
        return Result.ok(payService.notify(payNo, tradeNo, amount, sign));
    }

    @Operation(summary = "查询订单支付单")
    @GetMapping("/order/{orderNo}")
    public Result<Payment> byOrder(@PathVariable String orderNo) {
        return Result.ok(payService.byOrder(currentUser.userId(), orderNo));
    }
}
