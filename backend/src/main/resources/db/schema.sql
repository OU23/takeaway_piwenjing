-- =====================================================================
-- 数创星光外卖 · 原型数据库结构（单端下单链路）
-- 金额一律以「分」为单位存整数（SRS 9.7）
-- =====================================================================
CREATE DATABASE IF NOT EXISTS xingguang_delivery
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE xingguang_delivery;

-- ---------------------------------------------------------------------
-- 商家
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS merchant;
CREATE TABLE merchant (
    id         BIGINT      NOT NULL COMMENT '主键',
    name       VARCHAR(64) NOT NULL COMMENT '商家名称',
    contact    VARCHAR(32)          COMMENT '联系人',
    phone      VARCHAR(20)          COMMENT '联系电话',
    commission_rate INT   NOT NULL DEFAULT 800 COMMENT '技术服务费万分比, 800=8%',
    status     TINYINT     NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    deleted    TINYINT     NOT NULL DEFAULT 0,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='商家';

-- ---------------------------------------------------------------------
-- 门店（含双履约配置：平台配送 / 到店自取）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS shop;
CREATE TABLE shop (
    id             BIGINT       NOT NULL,
    merchant_id    BIGINT       NOT NULL,
    name           VARCHAR(64)  NOT NULL,
    header_img     VARCHAR(255)          COMMENT '头图(渐变占位)',
    logo           VARCHAR(255),
    rating         DECIMAL(2, 1) NOT NULL DEFAULT 4.8,
    month_sales    INT          NOT NULL DEFAULT 0,
    address        VARCHAR(160),
    lat            DECIMAL(10, 6),
    lng            DECIMAL(10, 6),
    notice         VARCHAR(255),
    open_time      VARCHAR(8)   NOT NULL DEFAULT '09:00',
    close_time     VARCHAR(8)   NOT NULL DEFAULT '21:00',
    status         VARCHAR(16)  NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN营业 CLOSED打烊 REST休息',
    min_price      INT          NOT NULL DEFAULT 0 COMMENT '起送价(分)',
    delivery_fee   INT          NOT NULL DEFAULT 0 COMMENT '配送费(分)',
    pack_fee       INT          NOT NULL DEFAULT 0 COMMENT '打包费(分)/件',
    eta_minutes    INT          NOT NULL DEFAULT 30 COMMENT '预计送达分钟',
    pickup_enabled TINYINT      NOT NULL DEFAULT 1 COMMENT '是否支持到店自取',
    pickup_discount INT         NOT NULL DEFAULT 0 COMMENT '自取立减(分)',
    distance_km    DECIMAL(4, 1) NOT NULL DEFAULT 1.0 COMMENT '演示用距离',
    tags           VARCHAR(160)          COMMENT '活动标签(逗号分隔)',
    deleted        TINYINT      NOT NULL DEFAULT 0,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_merchant (merchant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='门店';

-- ---------------------------------------------------------------------
-- 平台品类（首页金刚区）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS platform_category;
CREATE TABLE platform_category (
    id     BIGINT      NOT NULL,
    name   VARCHAR(32) NOT NULL,
    icon   VARCHAR(32)          COMMENT '图标标识',
    sort   INT         NOT NULL DEFAULT 0,
    status TINYINT     NOT NULL DEFAULT 1,
    deleted TINYINT    NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='平台品类';

-- ---------------------------------------------------------------------
-- 店内商品分类
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS dish_category;
CREATE TABLE dish_category (
    id      BIGINT      NOT NULL,
    shop_id BIGINT      NOT NULL,
    name    VARCHAR(32) NOT NULL,
    sort    INT         NOT NULL DEFAULT 0,
    deleted TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_shop (shop_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='店内商品分类';

-- ---------------------------------------------------------------------
-- 商品 SPU
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS dish;
CREATE TABLE dish (
    id           BIGINT       NOT NULL,
    shop_id      BIGINT       NOT NULL,
    category_id  BIGINT       NOT NULL,
    name         VARCHAR(64)  NOT NULL,
    description  VARCHAR(255),
    image        VARCHAR(255),
    price        INT          NOT NULL COMMENT '单价(分)',
    origin_price INT                   COMMENT '划线价(分)',
    month_sales  INT          NOT NULL DEFAULT 0,
    stock        INT          NOT NULL DEFAULT 999,
    sold_out     TINYINT      NOT NULL DEFAULT 0 COMMENT '1已估清',
    status       VARCHAR(16)  NOT NULL DEFAULT 'ON_SALE' COMMENT 'ON_SALE上架 OFF_SHELF下架',
    tags         VARCHAR(64)           COMMENT '招牌/新品/热销',
    sort         INT          NOT NULL DEFAULT 0,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_shop_cat (shop_id, category_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='商品SPU';

-- ---------------------------------------------------------------------
-- 规格 / 加料（同一商品下按 group_name 分组）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS dish_spec;
CREATE TABLE dish_spec (
    id         BIGINT      NOT NULL,
    dish_id    BIGINT      NOT NULL,
    group_name VARCHAR(32) NOT NULL COMMENT '规格组名，如 杯型/辣度',
    name       VARCHAR(32) NOT NULL COMMENT '规格项名，如 大杯',
    price_delta INT        NOT NULL DEFAULT 0 COMMENT '加价(分)',
    type       VARCHAR(8)  NOT NULL DEFAULT 'SPEC' COMMENT 'SPEC规格 ADDON加料',
    is_default TINYINT     NOT NULL DEFAULT 0,
    sort       INT         NOT NULL DEFAULT 0,
    deleted    TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_dish (dish_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='商品规格/加料';

-- ---------------------------------------------------------------------
-- 购物车
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS cart_item;
CREATE TABLE cart_item (
    id         BIGINT       NOT NULL,
    user_id    BIGINT       NOT NULL,
    shop_id    BIGINT       NOT NULL,
    dish_id    BIGINT       NOT NULL,
    spec_ids   VARCHAR(160)          COMMENT '规格id,逗号分隔',
    spec_text  VARCHAR(160)          COMMENT '规格文本快照',
    unit_price INT          NOT NULL COMMENT '含规格加价的单价(分)',
    quantity   INT          NOT NULL DEFAULT 1,
    deleted    TINYINT      NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_shop (user_id, shop_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='购物车';

-- ---------------------------------------------------------------------
-- 用户
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS app_user;
CREATE TABLE app_user (
    id            BIGINT       NOT NULL,
    username      VARCHAR(32)  DEFAULT NULL COMMENT '登录账号，3-20 位字母数字下划线',
    email         VARCHAR(128) DEFAULT NULL COMMENT '邮箱，可登录',
    password_hash VARCHAR(255) DEFAULT NULL COMMENT 'PBKDF2 摘要，第三方登录为空',
    phone         VARCHAR(20)  DEFAULT NULL COMMENT '手机号，账号注册用户可为空',
    nickname      VARCHAR(32),
    avatar        VARCHAR(255),
    balance       INT          NOT NULL DEFAULT 0 COMMENT '钱包余额(分)',
    member_level  VARCHAR(16)  NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/SILVER/GOLD/PLATINUM/BLACKGOLD/BLACKDIAMOND',
    growth        INT          NOT NULL DEFAULT 0 COMMENT '成长值',
    deleted       TINYINT      NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email),
    UNIQUE KEY uk_phone (phone)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户';

-- ---------------------------------------------------------------------
-- 收货地址
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS user_address;
CREATE TABLE user_address (
    id         BIGINT       NOT NULL,
    user_id    BIGINT       NOT NULL,
    contact    VARCHAR(32)  NOT NULL,
    phone      VARCHAR(20)  NOT NULL,
    detail     VARCHAR(200) NOT NULL,
    tag        VARCHAR(16)           COMMENT '家/公司/学校',
    is_default TINYINT      NOT NULL DEFAULT 0,
    deleted    TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='收货地址';

-- ---------------------------------------------------------------------
-- 订单
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS orders;
CREATE TABLE orders (
    id               BIGINT       NOT NULL,
    order_no         VARCHAR(32)  NOT NULL,
    user_id          BIGINT       NOT NULL,
    shop_id          BIGINT       NOT NULL,
    shop_name        VARCHAR(64)  NOT NULL,
    shop_header_img  VARCHAR(255),
    rider_id         BIGINT                DEFAULT NULL COMMENT '骑手id,自取单为空',
    fulfill_type     VARCHAR(16)  NOT NULL COMMENT 'DELIVERY平台配送 PICKUP到店自取',
    status           VARCHAR(24)  NOT NULL COMMENT '订单状态机，见 SRS 8.9',
    item_amount      INT          NOT NULL DEFAULT 0 COMMENT '商品小计(分)',
    pack_fee         INT          NOT NULL DEFAULT 0 COMMENT '打包费(分)',
    delivery_fee     INT          NOT NULL DEFAULT 0 COMMENT '配送费(分)，自取单为0',
    discount_amount  INT          NOT NULL DEFAULT 0 COMMENT '优惠合计(分)',
    pay_amount       INT          NOT NULL DEFAULT 0 COMMENT '实付(分)',
    receiver_name    VARCHAR(32),
    receiver_phone   VARCHAR(20),
    receiver_address VARCHAR(220),
    receiver_lat     DECIMAL(10,6)         COMMENT '收货地址纬度(地理编码缓存)',
    receiver_lng     DECIMAL(10,6)         COMMENT '收货地址经度(地理编码缓存)',
    pickup_code      VARCHAR(16)           COMMENT '取餐码(自取单)',
    remark           VARCHAR(255),
    promise_minutes  INT,
    cancel_reason    VARCHAR(128),
    created_at       DATETIME     NOT NULL,
    paid_at          DATETIME,
    finished_at      DATETIME,
    deleted          TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_status (user_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='订单';

-- ---------------------------------------------------------------------
-- 订单明细
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS order_item;
CREATE TABLE order_item (
    id         BIGINT      NOT NULL,
    order_id   BIGINT      NOT NULL,
    order_no   VARCHAR(32) NOT NULL,
    dish_id    BIGINT      NOT NULL,
    dish_name  VARCHAR(64) NOT NULL,
    dish_image VARCHAR(255),
    spec_text  VARCHAR(160),
    unit_price INT         NOT NULL COMMENT '下单时单价(分)',
    quantity   INT         NOT NULL,
    amount     INT         NOT NULL COMMENT '小计(分)',
    deleted    TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='订单明细';

-- ---------------------------------------------------------------------
-- 订单状态流水
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS order_status_log;
CREATE TABLE order_status_log (
    id          BIGINT      NOT NULL,
    order_id    BIGINT      NOT NULL,
    order_no    VARCHAR(32) NOT NULL,
    from_status VARCHAR(24),
    to_status   VARCHAR(24) NOT NULL,
    operator    VARCHAR(32) NOT NULL DEFAULT 'SYSTEM',
    remark      VARCHAR(128),
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='订单状态流水';

-- ---------------------------------------------------------------------
-- 支付单
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS payment;
CREATE TABLE payment (
    id         BIGINT      NOT NULL,
    pay_no     VARCHAR(32) NOT NULL,
    order_id   BIGINT      NOT NULL,
    order_no   VARCHAR(32) NOT NULL,
    channel    VARCHAR(24) NOT NULL COMMENT 'ALIPAY_SANDBOX/WECHAT_MOCK/BALANCE',
    amount     INT         NOT NULL,
    status     VARCHAR(16) NOT NULL DEFAULT 'WAIT' COMMENT 'WAIT/SUCCESS/FAIL/CLOSED/REFUNDED',
    trade_no   VARCHAR(64)          COMMENT '渠道交易号',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at    DATETIME,
    refunded_at DATETIME             COMMENT '退款时间，仅 REFUNDED 有值',
    deleted    TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pay_no (pay_no),
    KEY idx_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='支付单';

-- ---------------------------------------------------------------------
-- 幂等记录（下单/支付/核销必带 Idempotency-Key，SRS 9.6）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS idempotency_record;
CREATE TABLE idempotency_record (
    id         BIGINT      NOT NULL,
    idem_key   VARCHAR(80) NOT NULL,
    scene      VARCHAR(32) NOT NULL,
    result_ref VARCHAR(64),
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_key_scene (idem_key, scene)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='幂等记录';

-- ---------------------------------------------------------------------
-- 骑手（SRS 1.4 骑手端 App）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS rider;
CREATE TABLE rider (
    id            BIGINT       NOT NULL,
    name          VARCHAR(32)  NOT NULL COMMENT '骑手姓名',
    phone         VARCHAR(20)  NOT NULL COMMENT '联系电话',
    avatar        VARCHAR(255)          COMMENT '头像(渐变占位)',
    status        VARCHAR(16)  NOT NULL DEFAULT 'OFFLINE' COMMENT 'ONLINE接单中 OFFLINE下班',
    rating        DECIMAL(2,1) NOT NULL DEFAULT 4.9 COMMENT '评分',
    vehicle       VARCHAR(32)  NOT NULL DEFAULT '电动车' COMMENT '配送工具',
    id_card       VARCHAR(24)           COMMENT '身份证号(脱敏存储,仅后台可见)',
    completed_count INT        NOT NULL DEFAULT 0 COMMENT '累计完成单量',
    balance       INT          NOT NULL DEFAULT 0 COMMENT '钱包余额(分),含已结算收入',
    deleted       TINYINT      NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_rider_phone (phone)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='骑手';

-- ---------------------------------------------------------------------
-- 骑手收入明细（每单结算：配送费佣金 + 平台补贴 - 罚款）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS rider_income;
CREATE TABLE rider_income (
    id            BIGINT       NOT NULL,
    rider_id      BIGINT       NOT NULL,
    order_id      BIGINT       NOT NULL,
    order_no      VARCHAR(32)  NOT NULL,
    shop_name     VARCHAR(64)  NOT NULL,
    base_fee      INT          NOT NULL DEFAULT 0 COMMENT '配送费佣金(分)',
    subsidy       INT          NOT NULL DEFAULT 0 COMMENT '平台补贴(分)',
    penalty       INT          NOT NULL DEFAULT 0 COMMENT '罚款(分),可为负扣减',
    income_amount INT          NOT NULL DEFAULT 0 COMMENT '实得收入(分)=base_fee+subsidy-penalty',
    settled       TINYINT      NOT NULL DEFAULT 0 COMMENT '1已结算 0待结算',
    settled_at    DATETIME              COMMENT '结算时间',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_rider (rider_id),
    KEY idx_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='骑手收入明细';

-- ---------------------------------------------------------------------
-- 骑手消息（系统 / 订单）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS rider_message;
CREATE TABLE rider_message (
    id           BIGINT       NOT NULL,
    rider_id     BIGINT       NOT NULL,
    type         VARCHAR(16)  NOT NULL DEFAULT 'SYSTEM' COMMENT 'SYSTEM系统 ORDER订单',
    title        VARCHAR(64)  NOT NULL,
    content      VARCHAR(255) NOT NULL,
    ref_order_no VARCHAR(32)           COMMENT '关联订单号',
    is_read      TINYINT      NOT NULL DEFAULT 0 COMMENT '1已读 0未读',
    deleted      TINYINT      NOT NULL DEFAULT 0,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_rider (rider_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='骑手消息';

-- ---------------------------------------------------------------------
-- 运营后台账号（RBAC，SRS 平台运营后台）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS admin_user;
CREATE TABLE admin_user (
    id           BIGINT       NOT NULL,
    username     VARCHAR(32)  NOT NULL COMMENT '登录账号',
    password     VARCHAR(64)  NOT NULL DEFAULT 'e10adc3949ba59abbe56e057f20f883e' COMMENT 'MD5(密码),默认 123456',
    name         VARCHAR(32)  NOT NULL COMMENT '姓名',
    role         VARCHAR(16)  NOT NULL COMMENT 'SUPER_ADMIN超级管理员 OPERATOR运营 CS客服',
    phone        VARCHAR(20),
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    last_login   DATETIME,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='运营后台账号';

-- ---------------------------------------------------------------------
-- 商家端登录账号（按门店隔离：一个账号管理一家店，v1）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS merchant_user;
CREATE TABLE merchant_user (
    id           BIGINT       NOT NULL,
    merchant_id  BIGINT       NOT NULL COMMENT '关联 merchant.id',
    shop_id      BIGINT       NOT NULL COMMENT '该账号管理的门店',
    username     VARCHAR(32)  NOT NULL COMMENT '登录账号',
    password     VARCHAR(64)  NOT NULL DEFAULT 'e10adc3949ba59abbe56e057f20f883e' COMMENT 'MD5(密码),默认 123456',
    name         VARCHAR(32)  NOT NULL COMMENT '商家姓名/店长',
    phone        VARCHAR(20),
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    last_login   DATETIME,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_m_username (username),
    KEY idx_shop (shop_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='商家端账号';

-- ---------------------------------------------------------------------
-- 商家入驻申请（审核流）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS merchant_application;
CREATE TABLE merchant_application (
    id            BIGINT       NOT NULL,
    merchant_name VARCHAR(64)  NOT NULL COMMENT '申请商家名称',
    contact       VARCHAR(32)  NOT NULL COMMENT '联系人',
    phone         VARCHAR(20)  NOT NULL COMMENT '联系电话',
    category      VARCHAR(32)  NOT NULL COMMENT '经营品类',
    shop_name     VARCHAR(64)  NOT NULL COMMENT '申请门店名称',
    address       VARCHAR(160) NOT NULL COMMENT '门店地址',
    license_no    VARCHAR(64)           COMMENT '营业执照号',
    expect_commission INT      NOT NULL DEFAULT 800 COMMENT '期望技术服务费万分比',
    status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING待审核 APPROVED通过 REJECTED驳回',
    reject_reason VARCHAR(128)          COMMENT '驳回原因',
    reviewer_id   BIGINT                COMMENT '审核人 admin_user.id',
    reviewed_at   DATETIME              COMMENT '审核时间',
    deleted       TINYINT      NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='商家入驻申请';

-- ---------------------------------------------------------------------
-- 用户投诉（平台客诉）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS complaint;
CREATE TABLE complaint (
    id           BIGINT       NOT NULL,
    order_no     VARCHAR(32)           COMMENT '关联订单号',
    user_id      BIGINT       NOT NULL,
    user_name    VARCHAR(32),
    type         VARCHAR(16)  NOT NULL DEFAULT 'OTHER' COMMENT 'REFUND退款 DELAY超时 QUALITY质量 SERVICE服务 OTHER其他',
    level        VARCHAR(8)   NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL普通 URGENT紧急',
    content      VARCHAR(255) NOT NULL COMMENT '投诉内容',
    status       VARCHAR(16)  NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN待处理 HANDLING处理中 RESOLVED已解决 CLOSED已关闭',
    handler_id   BIGINT                COMMENT '处理人 admin_user.id',
    handle_note  VARCHAR(255),
    deleted      TINYINT      NOT NULL DEFAULT 0,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_order (order_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户投诉';

-- ---------------------------------------------------------------------
-- 订单评价（最小可用版：评分 + 文字 + 多图）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS review;
CREATE TABLE review (
    id         BIGINT       NOT NULL,
    order_id   BIGINT       NOT NULL,
    order_no   VARCHAR(40)  NOT NULL,
    user_id    BIGINT       NOT NULL,
    shop_id    BIGINT       NOT NULL,
    shop_name  VARCHAR(64)           COMMENT '门店名快照',
    rating     TINYINT      NOT NULL DEFAULT 5 COMMENT '评分1-5',
    content    VARCHAR(500)          COMMENT '评价文字',
    images     VARCHAR(1000)         COMMENT 'objectKey逗号分隔，最多9张',
    reply      VARCHAR(500)          COMMENT '商家回复',
    reply_at   DATETIME,
    deleted    TINYINT      NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order (order_id),
    KEY idx_shop (shop_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='订单评价';

-- ---------------------------------------------------------------------
-- 用户偏好设置（SRS 9.11 设置模块）
-- 一行一用户，settings 存 JSON；键与取值范围双侧校验（前端
-- store/settings-schema.js + 后端 UserSettingService.ALLOWED）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS user_setting;
CREATE TABLE user_setting (
    user_id    BIGINT   NOT NULL COMMENT '用户 id（app_user.id），一对一',
    settings   TEXT     NOT NULL COMMENT '设置 JSON，仅包含用户改过默认值的键',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户偏好设置';