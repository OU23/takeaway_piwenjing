package com.xingguang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 骑手收入明细（每单结算：配送费佣金 + 平台补贴 - 罚款）。金额单位均为「分」。 */
@Data
@TableName("rider_income")
public class RiderIncome {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long riderId;
    private Long orderId;
    private String orderNo;
    private String shopName;
    /** 配送费佣金（分） */
    private Integer baseFee;
    /** 平台补贴（分） */
    private Integer subsidy;
    /** 罚款（分），可为负扣减 */
    private Integer penalty;
    /** 实得收入（分）= baseFee + subsidy - penalty */
    private Integer incomeAmount;
    /** 1 已结算 / 0 待结算 */
    private Integer settled;
    private LocalDateTime settledAt;

    private LocalDateTime createdAt;
}
