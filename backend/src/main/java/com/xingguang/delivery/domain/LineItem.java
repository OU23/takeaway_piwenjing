package com.xingguang.delivery.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一条「商品行」快照：来自购物车条目或直接下单参数。
 * 单价为「基础价 + 规格加价」快照，单位：分。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineItem {

    private Long dishId;
    private String dishName;
    private String dishImage;
    private String specText;

    /** 单价（分），含规格与加料加价 */
    private Integer unitPrice;
    private Integer quantity;

    public int amount() {
        return unitPrice * quantity;
    }
}
