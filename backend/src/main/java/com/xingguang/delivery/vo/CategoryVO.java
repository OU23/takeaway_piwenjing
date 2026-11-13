package com.xingguang.delivery.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 通用分类 VO（平台品类 / 店内分类共用）。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryVO {

    private Long id;
    private String name;
    private String icon;
    private Integer sort;

    public CategoryVO(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
