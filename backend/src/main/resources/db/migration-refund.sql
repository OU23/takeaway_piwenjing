-- 增量迁移：订单取消退款（2026-10-26）
--
-- 背景：余额支付后取消订单，钱只扣不退。payment 表也没有任何字段能表达「已退款」。
-- 本次补齐：payment.status 增加 REFUNDED，并记录退款时间。
--
-- 用法：mysql -uroot -p xingguang_delivery < migration-refund.sql
-- 可重复执行（先查 information_schema 再决定是否加列）。

SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'payment' AND COLUMN_NAME = 'refunded_at'
);
SET @ddl := IF(@col = 0,
    'ALTER TABLE payment ADD COLUMN refunded_at DATETIME NULL COMMENT ''退款时间，仅 REFUNDED 有值'' AFTER paid_at',
    'SELECT ''refunded_at 已存在，跳过'' AS msg');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 同步状态注释（MySQL 8 支持 MODIFY 改注释，不影响数据）
ALTER TABLE payment
    MODIFY COLUMN status VARCHAR(16) NOT NULL DEFAULT 'WAIT'
    COMMENT 'WAIT/SUCCESS/FAIL/CLOSED/REFUNDED';
