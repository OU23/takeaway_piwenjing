package com.xingguang.delivery.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 购物车（按门店维度组织）。 */
@Data
public class CartVO {

    private Long shopId;
    private String shopName;
    private String shopHeaderImg;
    /** 起送价（分） */
    private Integer minPrice;

    private List<Line> lines = new ArrayList<>();

    /** 商品小计（分） */
    private Integer itemAmount = 0;
    /** 商品总件数 */
    private Integer totalQuantity = 0;

    @Data
    public static class Line {
        private Long id;
        private Long dishId;
        private String dishName;
        private String dishImage;
        private String specText;
        /** 单价（分） */
        private Integer unitPrice;
        private Integer quantity;
        /** 小计（分） */
        private Integer amount;
    }
}
