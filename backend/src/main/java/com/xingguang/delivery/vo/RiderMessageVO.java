package com.xingguang.delivery.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 骑手消息视图。 */
@Data
public class RiderMessageVO {

    private Long id;
    /** SYSTEM / ORDER */
    private String type;
    private String title;
    private String content;
    private String refOrderNo;
    private Integer isRead;
    private LocalDateTime createdAt;
}
