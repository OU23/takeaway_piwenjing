package com.xingguang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 购物车条目。unitPrice 为「基础价 + 规格加价」的快照单价（分）。 */
@Data
@TableName("cart_item")
public class CartItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private Long shopId;
    private Long dishId;

    /** 规格 id，逗号分隔 */
    private String specIds;
    /** 规格文本快照，如「中杯 / 少冰 / 七分糖」 */
    private String specText;

    /** 含规格加价的单价（分） */
    private Integer unitPrice;
    private Integer quantity;

    private Integer deleted;
    private LocalDateTime createdAt;
}
