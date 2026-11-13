package com.xingguang.delivery.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 评价视图：图片已解析为可访问 URL。 */
@Data
public class ReviewVO {

    private Long id;
    private String orderNo;
    private Long shopId;
    private String shopName;

    /** 1-5 星 */
    private Integer rating;

    private String content;

    /** 已解析的可访问图片 URL 列表 */
    private List<String> images = new ArrayList<>();

    /** 商家回复 */
    private String reply;
    private LocalDateTime replyAt;

    /** 评价人昵称 / 头像（头像已解析为 URL） */
    private String nickname;
    private String userAvatar;

    private LocalDateTime createdAt;
}
