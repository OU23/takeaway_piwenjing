package com.xingguang.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 发起支付。 */
@Data
public class PayRequest {

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    /** ALIPAY_SANDBOX / WECHAT_MOCK / BALANCE */
    @NotBlank(message = "支付渠道不能为空")
    private String channel;
}
