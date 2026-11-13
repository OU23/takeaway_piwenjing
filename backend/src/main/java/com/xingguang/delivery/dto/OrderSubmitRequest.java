package com.xingguang.delivery.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 结算试算 / 提交订单共用入参。 */
@Data
public class OrderSubmitRequest {

    @NotNull(message = "门店不能为空")
    private Long shopId;

    /** DELIVERY 平台配送 / PICKUP 到店自取 */
    @NotNull(message = "履约方式不能为空")
    private String fulfillType;

    /** 收货地址 id；自取单可为空 */
    private Long addressId;

    /** 备注（口味 / 餐具份数等） */
    private String remark;

    /** 参与结算的购物车条目 id；为空表示该门店全部条目 */
    private List<Long> itemIds;
}
