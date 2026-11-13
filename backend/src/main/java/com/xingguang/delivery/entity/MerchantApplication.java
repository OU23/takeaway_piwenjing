package com.xingguang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 商家入驻申请（审核流）。 */
@Data
@TableName("merchant_application")
public class MerchantApplication {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String merchantName;
    private String contact;
    private String phone;
    private String category;
    private String shopName;
    private String address;
    private String licenseNo;
    /** 期望技术服务费万分比 */
    private Integer expectCommission;
    /** PENDING 待审核 / APPROVED 通过 / REJECTED 驳回 */
    private String status;
    private String rejectReason;
    private Long reviewerId;
    private LocalDateTime reviewedAt;
    private Integer deleted;
    private LocalDateTime createdAt;
}
