package com.xingguang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 商品 SPU。 */
@Data
@TableName("dish")
public class Dish {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long shopId;
    private Long categoryId;
    private String name;
    private String description;
    private String image;

    /** 单价（分） */
    private Integer price;
    /** 划线价（分），可空 */
    private Integer originPrice;

    private Integer monthSales;
    private Integer stock;
    /** 1 已估清（不参与售卖） */
    private Integer soldOut;
    /** ON_SALE 上架 / OFF_SHELF 下架 */
    private String status;

    /** 招牌 / 新品 / 热销 */
    private String tags;
    private Integer sort;
    private Integer deleted;
}
