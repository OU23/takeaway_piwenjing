package com.xingguang.delivery.vo;

import lombok.Data;

/** 收货地址（手机号脱敏展示）。 */
@Data
public class AddressVO {

    private Long id;
    private String contact;
    /** 脱敏手机号 */
    private String phoneMasked;
    private String detail;
    private String tag;
    private Boolean isDefault;
}
