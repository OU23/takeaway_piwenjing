package com.xingguang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单评价（最小可用版）：订单完成后评分 + 文字 + 多图。
 *
 * <p>一个订单只能评价一次（库表 uk_order 唯一键 + 服务层校验双重保证）。
 * <p>图片列存 objectKey（逗号分隔，最多 9 张），展示地址由 ObjectStorageService 生成。
 */
@Data
@TableName("review")
public class Review {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;
    private String orderNo;
    private Long userId;
    private Long shopId;

    /** 门店名快照，避免「我的评价」再回表关联门店 */
    private String shopName;

    /** 评分 1-5 星 */
    private Integer rating;

    private String content;

    /** 图片 objectKey，逗号分隔（最多 9 张），可为空 */
    private String images;

    /** 商家回复 */
    private String reply;
    private LocalDateTime replyAt;

    private Integer deleted;
    private LocalDateTime createdAt;
}
