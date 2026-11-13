package com.xingguang.delivery.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 可用支付渠道。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayChannelVO {

    private String code;
    private String label;
    /** 是否为模拟通道（前端需展示"模拟支付"提示，SRS 9.3） */
    private Boolean mock;
    /** 说明（对应 SRS 1.5 的【保留】/【替换】结论） */
    private String remark;
    /** 当前是否可用 */
    private Boolean available;
}
