package com.xingguang.delivery.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 加入购物车。 */
@Data
public class AddCartRequest {

    @NotNull(message = "商品不能为空")
    private Long dishId;

    /** 选中的规格 / 加料 id 列表，可为空（无规格商品） */
    private List<Long> specIds;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为 1")
    @Max(value = 99, message = "单个商品数量上限 99")
    private Integer quantity;
}
