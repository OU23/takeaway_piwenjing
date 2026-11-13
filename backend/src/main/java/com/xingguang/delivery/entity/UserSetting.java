package com.xingguang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户偏好设置（SRS 9.11）。一行一用户，settings 为 JSON 字符串。
 *
 * 键结构与默认值由前端 store/settings-schema.js 维护；后端只做
 * 「白名单键 + 类型/取值范围」校验，未登记的键直接丢弃，
 * 因此新增设置项不需要改表结构。
 */
@Data
@TableName("user_setting")
public class UserSetting {

    /** 用户 id（app_user.id），一对一主键 */
    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;

    /** 设置 JSON（仅包含用户改过默认值的键） */
    private String settings;

    private LocalDateTime updatedAt;
}
