package com.xingguang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户投诉（平台客诉）。 */
@Data
@TableName("complaint")
public class Complaint {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;
    private Long userId;
    private String userName;
    /** REFUND 退款 / DELAY 超时 / QUALITY 质量 / SERVICE 服务 / OTHER 其他 */
    private String type;
    /** NORMAL 普通 / URGENT 紧急 */
    private String level;
    private String content;
    /** OPEN 待处理 / HANDLING 处理中 / RESOLVED 已解决 / CLOSED 已关闭 */
    private String status;
    private Long handlerId;
    private String handleNote;
    private Integer deleted;
    private LocalDateTime createdAt;
}
