package com.xingguang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 订单状态流水（状态机可追溯，SRS 3.2 审计）。 */
@Data
@TableName("order_status_log")
public class OrderStatusLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;
    private String orderNo;
    private String fromStatus;
    private String toStatus;

    /** USER / MERCHANT / RIDER / SYSTEM / ADMIN */
    private String operator;
    private String remark;
    private LocalDateTime createdAt;
}
