-- =====================================================================
-- 增量迁移：C 端账号体系（登录 / 注册）
-- 适用于已经跑过旧版 schema.sql 的库；全新库直接执行 schema.sql 即可。
-- 只做加法，不删除既有数据。
-- =====================================================================

ALTER TABLE app_user
    ADD COLUMN username      VARCHAR(32)  DEFAULT NULL COMMENT '登录账号，3-20 位字母数字下划线' AFTER id,
    ADD COLUMN email         VARCHAR(128) DEFAULT NULL COMMENT '邮箱，可登录' AFTER username,
    ADD COLUMN password_hash VARCHAR(255) DEFAULT NULL COMMENT 'PBKDF2 摘要，第三方登录为空' AFTER email;

-- 账号注册用户没有手机号，phone 不再作为必填身份
ALTER TABLE app_user MODIFY COLUMN phone VARCHAR(20) DEFAULT NULL COMMENT '手机号，账号注册用户可为空';

ALTER TABLE app_user
    ADD UNIQUE KEY uk_username (username),
    ADD UNIQUE KEY uk_email (email);
