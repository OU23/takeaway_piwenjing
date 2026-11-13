package com.xingguang.delivery.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 商品（含规格组与展示用派生字段）。 */
@Data
public class DishVO {

    private Long id;
    private Long shopId;
    private Long categoryId;
    private String name;
    private String description;
    private String image;

    /** 单价（分） */
    private Integer price;
    /** 划线价（分） */
    private Integer originPrice;
    private Integer monthSales;
    private Integer stock;
    private Boolean soldOut;
    private List<String> tags = new ArrayList<>();

    /** 规格组；为空表示无规格，可直接加购 */
    private List<SpecGroupVO> specGroups = new ArrayList<>();

    /** 是否有规格可选（前端据此决定是直接加购还是弹规格弹窗） */
    private Boolean hasSpec;
}
