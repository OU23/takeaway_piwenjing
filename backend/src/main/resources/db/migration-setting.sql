-- =====================================================================
-- 增量迁移：用户偏好设置（SRS 9.11 设置模块）
-- 适用于已经跑过旧版 schema.sql 的库；全新库直接执行 schema.sql 即可。
-- 只做加法，不删除既有数据。
--
-- 设计说明：
-- · 一行一用户，settings 存 JSON 对象（按分类分组的扁平键值）。
--   相比 key-value 行式存储，用户设置是低频小数据（<1KB），整存整取
--   读写最简单，且新增设置项无需改表结构——前端/后端各自维护
--   「默认值 + 白名单校验」，未知键在写入时直接丢弃。
-- · 键与取值范围由前端 store/settings-schema.js 与后端
--   UserSettingService.ALLOWED 共同约束，双侧一致。
-- =====================================================================

CREATE TABLE IF NOT EXISTS user_setting (
    user_id    BIGINT   NOT NULL COMMENT '用户 id（app_user.id），一对一',
    settings   TEXT     NOT NULL COMMENT '设置 JSON，仅包含用户改过默认值的键',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户偏好设置';
