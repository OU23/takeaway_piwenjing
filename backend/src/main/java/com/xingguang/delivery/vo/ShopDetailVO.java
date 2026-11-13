package com.xingguang.delivery.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 店铺详情：店铺信息 + 店内分类 + 商品（左分类右商品联动）。 */
@Data
public class ShopDetailVO {

    private ShopVO shop;
    private List<CategoryVO> categories = new ArrayList<>();
    private List<DishVO> dishes = new ArrayList<>();

    /** 购物车中该店铺的件数（用于角标） */
    private Integer cartQuantity = 0;
}
