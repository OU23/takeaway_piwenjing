package com.xingguang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 平台品类（首页金刚区），由后台驱动，前端不写死。 */
@Data
@TableName("platform_category")
public class PlatformCategory {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;
    private String icon;
    private Integer sort;
    private Integer status;
    private Integer deleted;
}
