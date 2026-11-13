package com.xingguang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("merchant")
public class Merchant {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;
    private String contact;
    private String phone;

    /** 技术服务费万分比，800 = 8%（SRS 4.8） */
    private Integer commissionRate;

    /** 1 启用 / 0 停用 */
    private Integer status;

    private Integer deleted;
    private LocalDateTime createdAt;
}
