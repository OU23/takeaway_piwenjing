package com.xingguang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 订单明细。下单时冻结商品名、规格文本与单价，避免商品变更影响历史订单。 */
@Data
@TableName("order_item")
public class OrderItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;
    private String orderNo;
    private Long dishId;
    private String dishName;
    private String dishImage;
    private String specText;

    /** 下单时单价（分） */
    private Integer unitPrice;
    private Integer quantity;
    /** 小计（分） */
    private Integer amount;

    private Integer deleted;
}
