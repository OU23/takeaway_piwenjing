package com.xingguang.delivery.vo;

import lombok.Data;

/** 发起支付的返回体。 */
@Data
public class PrepayVO {

    private String payNo;
    private String orderNo;
    private String channel;
    private String channelLabel;
    /** 应付金额（分） */
    private Integer amount;
    /** 是否模拟通道：前端据此直接调 mock-notify 完成"一键支付成功" */
    private Boolean mock;
    /** 真实通道（支付宝沙箱）时返回的表单/跳转地址 */
    private String payUrl;
    /** 支付单状态：WAIT / SUCCESS */
    private String status;
}
