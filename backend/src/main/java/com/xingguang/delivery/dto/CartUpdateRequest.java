package com.xingguang.delivery.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 修改购物车条目数量。 */
@Data
public class CartUpdateRequest {

    @NotNull(message = "数量不能为空")
    @Min(value = 0, message = "数量不能为负数")
    @Max(value = 99, message = "单个商品数量上限 99")
    private Integer quantity;
}
