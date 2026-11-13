package com.xingguang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 商品规格 / 加料。同一商品下按 groupName 分组。 */
@Data
@TableName("dish_spec")
public class DishSpec {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long dishId;

    /** 规格组名，如「杯型」「辣度」 */
    private String groupName;
    /** 规格项名，如「大杯」 */
    private String name;

    /** 加价（分） */
    private Integer priceDelta;

    /** SPEC 规格 / ADDON 加料 */
    private String type;
    private Integer isDefault;
    private Integer sort;
    private Integer deleted;
}
