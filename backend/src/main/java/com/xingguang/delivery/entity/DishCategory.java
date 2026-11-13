package com.xingguang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 店内商品分类（店铺页左侧锚点）。 */
@Data
@TableName("dish_category")
public class DishCategory {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long shopId;
    private String name;
    private Integer sort;
    private Integer deleted;
}
