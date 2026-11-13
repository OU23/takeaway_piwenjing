package com.xingguang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 幂等记录：写接口携带 Idempotency-Key（下单、支付、核销必带）。 */
@Data
@TableName("idempotency_record")
public class IdempotencyRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String idemKey;
    /** 场景：ORDER_CREATE / ORDER_PAY ... */
    private String scene;
    /** 幂等命中时返回的原结果引用（如订单号） */
    private String resultRef;
    private LocalDateTime createdAt;
}
