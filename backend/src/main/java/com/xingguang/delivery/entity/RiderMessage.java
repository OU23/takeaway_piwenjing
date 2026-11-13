package com.xingguang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 骑手消息（系统 / 订单）。 */
@Data
@TableName("rider_message")
public class RiderMessage {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long riderId;
    /** SYSTEM 系统 / ORDER 订单 */
    private String type;
    private String title;
    private String content;
    private String refOrderNo;
    /** 1 已读 / 0 未读 */
    private Integer isRead;

    private Integer deleted;
    private LocalDateTime createdAt;
}
