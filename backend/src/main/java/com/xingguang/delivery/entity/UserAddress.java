package com.xingguang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 收货地址。自取单不使用地址，改为展示门店信息。 */
@Data
@TableName("user_address")
public class UserAddress {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private String contact;
    private String phone;
    private String detail;

    /** 家 / 公司 / 学校 */
    private String tag;
    private Integer isDefault;
    private Integer deleted;
}
