-- =====================================================================
-- 数创星光外卖 · 演示数据
-- 说明：所有金额单位为「分」。门店头图 / 商品图使用渐变占位（前端渲染）。
-- =====================================================================
USE xingguang_delivery;

DELETE FROM merchant;
DELETE FROM shop;
DELETE FROM platform_category;
DELETE FROM dish_category;
DELETE FROM dish;
DELETE FROM dish_spec;
DELETE FROM cart_item;
DELETE FROM app_user;
DELETE FROM user_address;
DELETE FROM order_item;
DELETE FROM order_status_log;
DELETE FROM orders;
DELETE FROM payment;
DELETE FROM idempotency_record;
DELETE FROM rider_message;
DELETE FROM rider_income;
DELETE FROM rider;
DELETE FROM complaint;
DELETE FROM merchant_application;
DELETE FROM merchant_user;
DELETE FROM admin_user;

-- ------------------------------ 商家 ------------------------------
INSERT INTO merchant (id, name, contact, phone, commission_rate, status) VALUES
(1, '星光餐饮管理有限公司', '王经理', '022-8800-1001', 800, 1),
(2, '星选轻食（天津）有限公司', '刘店长', '022-8800-1002', 750, 1),
(3, '蜜茶时光饮品有限公司', '陈老板', '022-8800-1003', 700, 1);

-- ------------------------------ 门店 ------------------------------
INSERT INTO shop (id, merchant_id, name, logo, header_img, rating, month_sales, address, lat, lng, notice,
                  open_time, close_time, status, min_price, delivery_fee, pack_fee, eta_minutes,
                  pickup_enabled, pickup_discount, distance_km, tags) VALUES
(101, 1, '星光小馆·川湘菜', 'grad-1', 'grad-1', 4.9, 2860, '滨海新区数创大厦 1F-08', 39.003100, 117.702100,
 '本店辣子鸡选用散养土鸡，现炒现做，预计出餐 15 分钟', '09:30', '21:30', 'OPEN', 2000, 300, 100, 32, 1, 300, 0.8,
 '满30减8,新客立减5,支持到店自取'),
(102, 2, '星选轻食·沙拉', 'grad-2', 'grad-2', 4.8, 1930, '滨海新区星光科技园 2 号楼 101', 39.005600, 117.706400,
 '全场低卡轻食，酱料免费加，支持到店自取立减 2 元', '10:00', '20:30', 'OPEN', 1800, 250, 50, 25, 1, 200, 1.2,
 '满25减5,会员专享,支持到店自取'),
(103, 3, '蜜茶时光·奶茶', 'grad-3', 'grad-3', 4.7, 5210, '滨海新区星光广场 B1-23', 39.007200, 117.699800,
 '鲜果现制，出杯约 8 分钟；到店自取立减 3 元', '10:00', '22:00', 'OPEN', 1500, 200, 100, 20, 1, 300, 1.5,
 '第二杯半价,满20减4,支持到店自取'),
(104, 1, '星光牛肉面', 'grad-4', 'grad-4', 4.6, 1420, '滨海新区幸福里小区 3 号商铺', 39.001900, 117.710300,
 '手工现拉面，汤底每日现熬', '07:00', '20:00', 'OPEN', 1600, 300, 100, 28, 0, 0, 2.1,
 '满20减3,招牌红烧牛肉面'),
(105, 1, '玫瑰情缘·鲜花店', 'grad-13', 'grad-13', 4.8, 860, '滨海新区星光大道 66 号 1F-12', 39.004800, 117.705200,
 '云南基地直供玫瑰，现包现送，支持到店自取', '08:30', '21:00', 'OPEN', 2900, 200, 0, 30, 1, 100, 1.8,
 '满59减10,鲜花速递,支持到店自取'),
(106, 1, 'CoCo都可', 'grad-12', 'grad-12', 4.9, 6800, '滨海新区数创大厦 B 座 1F-03', 39.000500, 117.701800,
 '全球经典茶饮，现点现做，满杯鲜果', '09:00', '22:00', 'OPEN', 1500, 0, 100, 15, 1, 100, 0.3,
 '满25减4,热销TOP1,支持到店自取'),
(107, 1, '蜜雪冰城', 'grad-18', 'grad-18', 4.7, 6000, '滨海新区星光步行街 8 号', 39.000800, 117.702300,
 '蜜雪冰城，平价好喝，甜蜜每一天', '09:00', '22:30', 'OPEN', 800, 0, 100, 15, 1, 100, 0.5,
 '2元甜筒,平价好喝,支持到店自取');

-- --------------------- 平台品类（首页金刚区 10 格） ---------------------
INSERT INTO platform_category (id, name, icon, sort) VALUES
(1, '美食',      'food',     1),
(2, '快餐',      'fast',     2),
(3, '甜品饮品',  'drink',    3),
(4, '生鲜果蔬',  'fresh',    4),
(5, '超市便利',  'market',   5),
(6, '医药健康',  'medical',  6),
(7, '鲜花蛋糕',  'cake',     7),
(8, '到店自取',  'pickup',   8),
(9, '天天神券',  'coupon',   9),
(10, '全部分类', 'all',     10);

-- ------------------------------ 店内分类 ------------------------------
INSERT INTO dish_category (id, shop_id, name, sort) VALUES
(201, 101, '招牌热菜', 1),
(202, 101, '特色小炒', 2),
(203, 101, '主食',     3),
(211, 102, '招牌沙拉', 1),
(212, 102, '轻食主食', 2),
(213, 102, '饮品',     3),
(221, 103, '人气奶茶', 1),
(222, 103, '果茶',     2),
(223, 103, '小食',     3),
(231, 104, '招牌面',   1),
(232, 104, '小菜',     2),
(241, 106, '人气推荐', 1),
(242, 106, '鲜果茶',   2),
(243, 106, '经典奶茶', 3),
(251, 107, '现制饮品', 1),
(252, 107, '冰淇淋',   2),
(253, 107, '果茶',     3);

-- ------------------------------ 商品 ------------------------------
INSERT INTO dish (id, shop_id, category_id, name, description, image, price, origin_price, month_sales, stock, sold_out, tags, sort) VALUES
-- 星光小馆·川湘菜
(301, 101, 201, '招牌辣子鸡', '散养土鸡现炒，麻辣鲜香，中辣起步', 'grad-a', 4800, 5600, 1280, 200, 0, '招牌,热销', 1),
(302, 101, 201, '麻婆豆腐',   '嫩豆腐配秘制豆瓣酱，麻辣下饭',       'grad-b', 2600, NULL,  860, 300, 0, '招牌',     2),
(303, 101, 201, '干锅花菜',   '有机花菜干锅做法，锅气十足',         'grad-c', 3200, NULL,  540, 200, 0, NULL,       3),
(304, 101, 202, '农家小炒肉', '本地黑猪五花肉，青椒爆炒',           'grad-d', 3800, NULL,  720, 200, 0, '热销',     1),
(305, 101, 202, '秘制回锅肉', '二刀肉配蒜苗，肥而不腻',             'grad-e', 4200, NULL,  410, 150, 1, NULL,       2),
(306, 101, 202, '川北凉粉',   '手工豌豆凉粉，红油爽滑',             'grad-f', 1800, NULL,  330, 200, 0, NULL,       3),
(307, 101, 203, '米饭',       '东北珍珠米饭，一碗',                 'grad-g', 300,  NULL,  2100, 999, 0, NULL,       1),
-- 星选轻食·沙拉
(321, 102, 211, '鸡胸肉藜麦沙拉', '低脂高蛋白，配藜麦与七种蔬菜',   'grad-h', 3600, 4200, 980, 200, 0, '招牌,会员专享', 1),
(322, 102, 211, '牛油果虾仁沙拉', '厄瓜多尔白虾 + 牛油果，油醋汁',  'grad-i', 4200, NULL,  620, 150, 0, '招牌',         2),
(323, 102, 212, '低脂鸡肉卷',     '全麦饼皮卷鸡胸肉与时蔬',         'grad-j', 2800, NULL,  740, 200, 0, '热销',         1),
(324, 102, 212, '全麦三明治',     '全麦面包 + 火腿蛋 + 生菜',       'grad-k', 2400, NULL,  560, 200, 0, NULL,           2),
(325, 102, 213, '鲜榨橙汁',       '当日现榨，无添加糖',             'grad-l', 1800, NULL,  430, 200, 0, NULL,           1),
-- 蜜茶时光·奶茶
(341, 103, 221, '招牌珍珠奶茶',   '手搓黑糖珍珠 + 鲜牛乳',           'grad-m', 1600, 2000, 3200, 500, 0, '招牌,热销', 1),
(342, 103, 221, '生椰拿铁',       '厚椰乳 + 意式浓缩',               'grad-n', 2000, NULL, 2600, 500, 0, '热销',     2),
(343, 103, 222, '芝士葡萄',       '现剥巨峰葡萄 + 芝士奶盖',         'grad-o', 2200, NULL, 1800, 300, 0, '招牌',     1),
(344, 103, 222, '满杯百香果',     '整颗百香果 + 四季春茶',           'grad-p', 1800, NULL, 1500, 300, 0, NULL,       2),
(345, 103, 221, '芋泥波波奶茶',   '手打芋泥 + 芋圆波波',             'grad-q', 1900, NULL, 2100, 400, 0, '新品',     3),
(346, 103, 223, '葡式蛋挞',       '现烤酥皮蛋挞，单个装',            'grad-r', 800,  NULL,  900, 300, 0, NULL,       1),
-- 星光牛肉面
(361, 104, 231, '招牌红烧牛肉面', '牛骨汤底 + 大块牛腩，手工拉面',   'grad-s', 2800, 3200, 1100, 300, 0, '招牌,热销', 1),
(362, 104, 231, '番茄牛腩面',     '番茄熬制汤底，酸甜开胃',          'grad-t', 3000, NULL,  640, 200, 0, NULL,       2),
(363, 104, 231, '清汤牛肉面',     '原汤清亮，配腱子肉片',            'grad-u', 2600, NULL,  520, 200, 0, NULL,       3),
(364, 104, 232, '凉拌海带丝',     '酸辣爽口解腻',                   'grad-v', 1000, NULL,  380, 300, 0, NULL,       1),
(365, 104, 232, '卤蛋',           '秘制卤汁浸泡，入味',              'grad-w', 300,  NULL,  760, 500, 0, NULL,       2),
-- CoCo都可
(381, 106, 241, '满杯莓莓',     '草莓 + 蔓越莓 + 四季春茶，满杯鲜果', 'grad-o', 2200, 2600, 4600, 500, 0, '招牌,热销', 1),
(382, 106, 241, '经典珍珠奶茶', '锡兰红茶 + 鲜牛乳 + 手作珍珠',       'grad-m', 1600, 2000, 3500, 500, 0, '招牌,热销', 2),
(383, 106, 242, '莓莓柠檬茶',   '新鲜柠檬手打 + 莓果风味',            'grad-q', 1800, NULL,  3200, 400, 0, '热销',     1),
(384, 106, 242, '百香果双响炮', '整颗百香果 + 椰果 + 脆啵啵',         'grad-p', 1900, NULL,  2800, 400, 0, NULL,       2),
(385, 106, 243, '生椰拿铁咖啡', '厚椰乳 + 意式浓缩，微苦回甘',        'grad-n', 2000, NULL,  2100, 300, 0, NULL,       1),
(386, 106, 242, '满杯红柚',     '西柚果粒 + 绿茶底，清爽微苦',        'grad-l', 1700, NULL,  1600, 300, 0, '新品',     3),
-- 蜜雪冰城
(391, 107, 251, '冰鲜柠檬水',     '整颗鲜柠手打，酸甜清爽',            'grad-l', 400, 600,  12000, 999, 0, '招牌',     1),
(392, 107, 251, '珍珠奶茶',       '锡兰红茶 + 鲜牛乳 + 手作珍珠',       'grad-m', 700, 900,  9000, 999, 0, '招牌',     2),
(393, 107, 251, '摇摇奶昔',       '香草冰淇淋 + 鲜奶现摇',             'grad-o', 800, NULL, 6000, 800, 0, '热销',     1),
(394, 107, 253, '摩天脆脆冰淇淋', '2 元甜筒，香脆蛋筒 + 软冰淇淋',      'grad-a', 200, NULL, 15000, 999, 0, '2元甜筒',  1);

-- --------------------------- 规格 / 加料 ---------------------------
INSERT INTO dish_spec (id, dish_id, group_name, name, price_delta, type, is_default, sort) VALUES
-- 辣子鸡：辣度 + 份量
(4001, 301, '辣度', '微辣',   0,    'SPEC',  0, 1),
(4002, 301, '辣度', '中辣',   0,    'SPEC',  1, 2),
(4003, 301, '辣度', '特辣',   0,    'SPEC',  0, 3),
(4004, 301, '份量', '标准份', 0,    'SPEC',  1, 1),
(4005, 301, '份量', '加大份', 1000, 'SPEC',  0, 2),
(4006, 301, '加料', '加土豆', 500,  'ADDON', 0, 1),
(4007, 301, '加料', '加藕片', 500,  'ADDON', 0, 2),
-- 奶茶类通用：杯型 / 温度 / 糖度 / 加料
(4010, 341, '杯型', '中杯',   0,   'SPEC',  1, 1),
(4011, 341, '杯型', '大杯',   300, 'SPEC',  0, 2),
(4012, 341, '温度', '热饮',   0,   'SPEC',  0, 1),
(4013, 341, '温度', '常温',   0,   'SPEC',  0, 2),
(4014, 341, '温度', '少冰',   0,   'SPEC',  1, 3),
(4015, 341, '糖度', '标准糖', 0,   'SPEC',  0, 1),
(4016, 341, '糖度', '七分糖', 0,   'SPEC',  1, 2),
(4017, 341, '糖度', '五分糖', 0,   'SPEC',  0, 3),
(4018, 341, '糖度', '无糖',   0,   'SPEC',  0, 4),
(4019, 341, '加料', '加珍珠', 200, 'ADDON', 0, 1),
(4020, 341, '加料', '加椰果', 200, 'ADDON', 0, 2),
(4021, 341, '加料', '加布丁', 300, 'ADDON', 0, 3),
(4022, 342, '杯型', '中杯',   0,   'SPEC',  1, 1),
(4023, 342, '杯型', '大杯',   300, 'SPEC',  0, 2),
(4024, 342, '温度', '热饮',   0,   'SPEC',  0, 1),
(4025, 342, '温度', '少冰',   0,   'SPEC',  1, 2),
(4026, 342, '糖度', '标准糖', 0,   'SPEC',  1, 1),
(4027, 342, '糖度', '无糖',   0,   'SPEC',  0, 2),
(4028, 342, '加料', '加奶盖', 400, 'ADDON', 0, 1),
-- 沙拉：酱料 + 加料
(4030, 321, '酱料', '油醋汁',   0,    'SPEC',  1, 1),
(4031, 321, '酱料', '千岛酱',   0,    'SPEC',  0, 2),
(4032, 321, '酱料', '芝麻酱',   200,  'SPEC',  0, 3),
(4033, 321, '加料', '双份鸡胸肉', 800,  'ADDON', 0, 1),
(4034, 321, '加料', '加牛油果',   1000, 'ADDON', 0, 2),
(4035, 322, '酱料', '油醋汁',   0,    'SPEC',  1, 1),
(4036, 322, '酱料', '芝麻酱',   200,  'SPEC',  0, 2),
(4037, 322, '加料', '加虾仁',   1000, 'ADDON', 0, 1),
-- 牛肉面：面型 + 加料
(4040, 361, '面型', '细面',     0,    'SPEC',  1, 1),
(4041, 361, '面型', '宽面',     0,    'SPEC',  0, 2),
(4042, 361, '面型', '刀削面',   0,    'SPEC',  0, 3),
(4043, 361, '加料', '加牛肉',   1000, 'ADDON', 0, 1),
(4044, 361, '加料', '加卤蛋',   300,  'ADDON', 0, 2),
(4045, 361, '加料', '加青菜',   200,  'ADDON', 0, 3),
-- CoCo都可：满杯莓莓 / 经典珍珠奶茶
(4050, 381, '杯型', '中杯',   0,   'SPEC',  1, 1),
(4051, 381, '杯型', '大杯',   300, 'SPEC',  0, 2),
(4052, 381, '温度', '少冰',   0,   'SPEC',  1, 1),
(4053, 381, '温度', '常温',   0,   'SPEC',  0, 2),
(4054, 381, '糖度', '标准糖', 0,   'SPEC',  0, 1),
(4055, 381, '糖度', '七分糖', 0,   'SPEC',  1, 2),
(4056, 381, '糖度', '无糖',   0,   'SPEC',  0, 3),
(4057, 381, '加料', '加椰果',   200, 'ADDON', 0, 1),
(4058, 381, '加料', '加脆啵啵', 300, 'ADDON', 0, 2),
(4059, 382, '杯型', '中杯',   0,   'SPEC',  1, 1),
(4060, 382, '杯型', '大杯',   300, 'SPEC',  0, 2),
(4061, 382, '温度', '热饮',   0,   'SPEC',  0, 1),
(4062, 382, '温度', '少冰',   0,   'SPEC',  1, 2),
(4063, 382, '糖度', '标准糖', 0,   'SPEC',  0, 1),
(4064, 382, '糖度', '七分糖', 0,   'SPEC',  1, 2),
(4065, 382, '加料', '加珍珠', 200, 'ADDON', 0, 1),
-- 蜜雪冰城：冰鲜柠檬水 / 珍珠奶茶
(4070, 391, '糖度', '标准糖', 0,   'SPEC',  1, 1),
(4071, 391, '糖度', '少糖',   0,   'SPEC',  0, 2),
(4072, 392, '杯型', '中杯',   0,   'SPEC',  1, 1),
(4073, 392, '杯型', '大杯',   200, 'SPEC',  0, 2),
(4074, 392, '温度', '少冰',   0,   'SPEC',  1, 1),
(4075, 392, '温度', '热饮',   0,   'SPEC',  0, 2),
(4076, 392, '糖度', '标准糖', 0,   'SPEC',  0, 1),
(4077, 392, '糖度', '无糖',   0,   'SPEC',  1, 2);

-- ------------------------------ 用户 ------------------------------
-- 演示账号：用户名 demo / 邮箱 demo@xingguang.com / 密码 demo1234
-- password_hash 为 PBKDF2-HMAC-SHA256（120000 轮）摘要，仅用于本地演示，勿在生产沿用。
INSERT INTO app_user (id, username, email, password_hash, phone, nickname, avatar, balance, member_level, growth) VALUES
(1, 'demo', 'demo@xingguang.com',
 'pbkdf2$120000$IRWIVBZhgBzUBhBwgW3FMw$kUm8fQinCoI6CqsFGBCbBmGKVBO3VDtJ3HdDMVUjKqk',
 '13800001111', '星光小星', 'grad-user', 30000, 'GOLD', 3000);

INSERT INTO user_address (id, user_id, contact, phone, detail, tag, is_default) VALUES
(1, 1, '张小明', '13800001111', '天津市滨海新区 数创大厦 A 座 1801', '家',   1),
(2, 1, '李工',   '13900002222', '天津市滨海新区 星光科技园 3 号楼 502', '公司', 0);

-- --------------------------- 演示历史订单 ---------------------------
-- 订单 A：平台配送 · 已完成
INSERT INTO orders (id, order_no, user_id, shop_id, shop_name, shop_header_img, rider_id, fulfill_type, status,
                    item_amount, pack_fee, delivery_fee, discount_amount, pay_amount,
                    receiver_name, receiver_phone, receiver_address, remark, promise_minutes,
                    created_at, paid_at, finished_at) VALUES
(9001, 'SG20261015100001', 1, 101, '星光小馆·川湘菜', 'grad-1', 1, 'DELIVERY', 'COMPLETED',
 5400, 300, 300, 800, 5200,
 '张小明', '138****1111', '天津市滨海新区 数创大厦 A 座 1801', '不要香菜，多给一副餐具', 32,
 '2026-10-15 11:20:00', '2026-10-15 11:20:35', '2026-10-15 12:02:10');

INSERT INTO order_item (id, order_id, order_no, dish_id, dish_name, dish_image, spec_text, unit_price, quantity, amount) VALUES
(9101, 9001, 'SG20261015100001', 301, '招牌辣子鸡', 'grad-a', '中辣 / 标准份', 4800, 1, 4800),
(9102, 9001, 'SG20261015100001', 307, '米饭',       'grad-g', NULL,            300,  2, 600);

INSERT INTO order_status_log (id, order_id, order_no, from_status, to_status, operator, remark, created_at) VALUES
(9201, 9001, 'SG20261015100001', NULL,          'UNPAID',    'USER',   '提交订单',   '2026-10-15 11:20:00'),
(9202, 9001, 'SG20261015100001', 'UNPAID',      'WAIT_ACCEPT','USER',  '支付成功',   '2026-10-15 11:20:35'),
(9203, 9001, 'SG20261015100001', 'WAIT_ACCEPT', 'PREPARING', 'MERCHANT','商家接单',   '2026-10-15 11:21:10'),
(9204, 9001, 'SG20261015100001', 'PREPARING',   'WAIT_PICKUP','MERCHANT','商家出餐',  '2026-10-15 11:36:40'),
(9205, 9001, 'SG20261015100001', 'WAIT_PICKUP', 'DELIVERING','RIDER',  '骑手取货',   '2026-10-15 11:42:05'),
(9206, 9001, 'SG20261015100001', 'DELIVERING',  'DELIVERED', 'RIDER',  '骑手送达',   '2026-10-15 12:01:30'),
(9207, 9001, 'SG20261015100001', 'DELIVERED',   'COMPLETED', 'USER',   '用户确认收货','2026-10-15 12:02:10');

INSERT INTO payment (id, pay_no, order_id, order_no, channel, amount, status, trade_no, created_at, paid_at) VALUES
(9301, 'PAY20261015100001', 9001, 'SG20261015100001', 'ALIPAY_SANDBOX', 5200, 'SUCCESS', '2026101522001', '2026-10-15 11:20:00', '2026-10-15 11:20:35');

-- 订单 B：平台配送 · 配送中
INSERT INTO orders (id, order_no, user_id, shop_id, shop_name, shop_header_img, rider_id, fulfill_type, status,
                    item_amount, pack_fee, delivery_fee, discount_amount, pay_amount,
                    receiver_name, receiver_phone, receiver_address, remark, promise_minutes,
                    created_at, paid_at) VALUES
(9002, 'SG20261016100002', 1, 103, '蜜茶时光·奶茶', 'grad-3', 1, 'DELIVERY', 'DELIVERING',
 3200, 200, 200, 400, 3200,
 '张小明', '138****1111', '天津市滨海新区 数创大厦 A 座 1801', '少冰七分糖', 20,
 '2026-10-16 09:40:00', '2026-10-16 09:40:22');

INSERT INTO order_item (id, order_id, order_no, dish_id, dish_name, dish_image, spec_text, unit_price, quantity, amount) VALUES
(9103, 9002, 'SG20261016100002', 341, '招牌珍珠奶茶', 'grad-m', '中杯 / 少冰 / 七分糖', 1600, 2, 3200);

INSERT INTO order_status_log (id, order_id, order_no, from_status, to_status, operator, remark, created_at) VALUES
(9211, 9002, 'SG20261016100002', NULL,          'UNPAID',     'USER',    '提交订单', '2026-10-16 09:40:00'),
(9212, 9002, 'SG20261016100002', 'UNPAID',      'WAIT_ACCEPT','USER',    '支付成功', '2026-10-16 09:40:22'),
(9213, 9002, 'SG20261016100002', 'WAIT_ACCEPT', 'PREPARING',  'MERCHANT','商家接单', '2026-10-16 09:41:05'),
(9214, 9002, 'SG20261016100002', 'PREPARING',   'WAIT_PICKUP','MERCHANT','商家出餐', '2026-10-16 09:49:30'),
(9215, 9002, 'SG20261016100002', 'WAIT_PICKUP', 'DELIVERING', 'RIDER',   '骑手取货', '2026-10-16 09:53:12');

INSERT INTO payment (id, pay_no, order_id, order_no, channel, amount, status, trade_no, created_at, paid_at) VALUES
(9302, 'PAY20261016100002', 9002, 'SG20261016100002', 'WECHAT_MOCK', 3200, 'SUCCESS', 'MOCK20261016001', '2026-10-16 09:40:00', '2026-10-16 09:40:22');

-- 订单 C：到店自取 · 待取餐
INSERT INTO orders (id, order_no, user_id, shop_id, shop_name, shop_header_img, fulfill_type, status,
                    item_amount, pack_fee, delivery_fee, discount_amount, pay_amount,
                    receiver_name, receiver_phone, receiver_address, pickup_code, remark, promise_minutes,
                    created_at, paid_at) VALUES
(9003, 'SG20261016100003', 1, 102, '星选轻食·沙拉', 'grad-2', 'PICKUP', 'WAIT_MEAL',
 3600, 50, 0, 200, 3450,
 '张小明', '138****1111', '星选轻食·沙拉（滨海新区星光科技园 2 号楼 101）', 'SG-8842', '油醋汁另装', 25,
 '2026-10-16 10:05:00', '2026-10-16 10:05:18');

INSERT INTO order_item (id, order_id, order_no, dish_id, dish_name, dish_image, spec_text, unit_price, quantity, amount) VALUES
(9104, 9003, 'SG20261016100003', 321, '鸡胸肉藜麦沙拉', 'grad-h', '油醋汁', 3600, 1, 3600);

INSERT INTO order_status_log (id, order_id, order_no, from_status, to_status, operator, remark, created_at) VALUES
(9221, 9003, 'SG20261016100003', NULL,          'UNPAID',     'USER',    '提交订单（到店自取）', '2026-10-16 10:05:00'),
(9222, 9003, 'SG20261016100003', 'UNPAID',      'WAIT_ACCEPT','USER',    '支付成功',             '2026-10-16 10:05:18'),
(9223, 9003, 'SG20261016100003', 'WAIT_ACCEPT', 'PREPARING',  'MERCHANT','商家接单',             '2026-10-16 10:06:02'),
(9224, 9003, 'SG20261016100003', 'PREPARING',   'WAIT_MEAL',  'MERCHANT','备餐完成，取餐码已下发', '2026-10-16 10:18:44');

INSERT INTO payment (id, pay_no, order_id, order_no, channel, amount, status, trade_no, created_at, paid_at) VALUES
(9303, 'PAY20261016100003', 9003, 'SG20261016100003', 'BALANCE', 3450, 'SUCCESS', 'BAL20261016001', '2026-10-16 10:05:00', '2026-10-16 10:05:18');

-- ------------------------------ 骑手 ------------------------------
INSERT INTO rider (id, name, phone, avatar, status, rating, vehicle, completed_count, balance) VALUES
(1, '王骑手', '13700008888', 'grad-rider', 'ONLINE', 4.9, '电动车', 2, 900);

-- ---------------------- 骑手端演示订单 ----------------------
-- 订单 D：平台配送 · 待取货（接单大厅可抢，未派单）
INSERT INTO orders (id, order_no, user_id, shop_id, shop_name, shop_header_img, rider_id, fulfill_type, status,
                    item_amount, pack_fee, delivery_fee, discount_amount, pay_amount,
                    receiver_name, receiver_phone, receiver_address, remark, promise_minutes,
                    created_at, paid_at) VALUES
(9004, 'SG20261020100004', 1, 101, '星光小馆·川湘菜', 'grad-1', NULL, 'DELIVERY', 'WAIT_PICKUP',
 2600, 200, 300, 500, 2600,
 '张小明', '138****1111', '天津市滨海新区 数创大厦 A 座 1801', '微辣，不要香菜', 32,
 '2026-10-20 11:55:00', '2026-10-20 11:55:30');

INSERT INTO order_item (id, order_id, order_no, dish_id, dish_name, dish_image, spec_text, unit_price, quantity, amount) VALUES
(9105, 9004, 'SG20261020100004', 302, '麻婆豆腐', 'grad-b', '微辣', 2600, 1, 2600);

INSERT INTO order_status_log (id, order_id, order_no, from_status, to_status, operator, remark, created_at) VALUES
(9225, 9004, 'SG20261020100004', NULL,          'UNPAID',     'USER',    '提交订单', '2026-10-20 11:55:00'),
(9226, 9004, 'SG20261020100004', 'UNPAID',      'WAIT_ACCEPT','USER',    '支付成功', '2026-10-20 11:55:30'),
(9227, 9004, 'SG20261020100004', 'WAIT_ACCEPT', 'PREPARING',  'MERCHANT','商家接单', '2026-10-20 11:56:10'),
(9228, 9004, 'SG20261020100004', 'PREPARING',   'WAIT_PICKUP','MERCHANT','商家出餐，等待骑手取货', '2026-10-20 12:08:30');

INSERT INTO payment (id, pay_no, order_id, order_no, channel, amount, status, trade_no, created_at, paid_at) VALUES
(9304, 'PAY20261020100004', 9004, 'SG20261020100004', 'WECHAT_MOCK', 2600, 'SUCCESS', 'MOCK20261020004', '2026-10-20 11:55:00', '2026-10-20 11:55:30');

-- 订单 F：平台配送 · 备餐中（接单大厅「即将取货」可抢，未派单）
INSERT INTO orders (id, order_no, user_id, shop_id, shop_name, shop_header_img, rider_id, fulfill_type, status,
                    item_amount, pack_fee, delivery_fee, discount_amount, pay_amount,
                    receiver_name, receiver_phone, receiver_address, remark, promise_minutes,
                    created_at, paid_at) VALUES
(9005, 'SG20261020100005', 1, 102, '星选轻食·沙拉', 'grad-2', NULL, 'DELIVERY', 'PREPARING',
 2800, 50, 250, 500, 2600,
 '李工', '139****2222', '天津市滨海新区 星光科技园 3 号楼 502', '酱料另装', 25,
 '2026-10-20 12:01:00', '2026-10-20 12:01:25');

INSERT INTO order_item (id, order_id, order_no, dish_id, dish_name, dish_image, spec_text, unit_price, quantity, amount) VALUES
(9106, 9005, 'SG20261020100005', 323, '低脂鸡肉卷', 'grad-j', NULL, 2800, 1, 2800);

INSERT INTO order_status_log (id, order_id, order_no, from_status, to_status, operator, remark, created_at) VALUES
(9230, 9005, 'SG20261020100005', NULL,          'UNPAID',     'USER',    '提交订单', '2026-10-20 12:01:00'),
(9231, 9005, 'SG20261020100005', 'UNPAID',      'WAIT_ACCEPT','USER',    '支付成功', '2026-10-20 12:01:25'),
(9232, 9005, 'SG20261020100005', 'WAIT_ACCEPT', 'PREPARING',  'MERCHANT','商家接单，正在备餐', '2026-10-20 12:02:05');

INSERT INTO payment (id, pay_no, order_id, order_no, channel, amount, status, trade_no, created_at, paid_at) VALUES
(9305, 'PAY20261020100005', 9005, 'SG20261020100005', 'BALANCE', 2600, 'SUCCESS', 'BAL20261020005', '2026-10-20 12:01:00', '2026-10-20 12:01:25');

-- 订单 E：平台配送 · 已完成（骑手历史单，已结算）
INSERT INTO orders (id, order_no, user_id, shop_id, shop_name, shop_header_img, rider_id, fulfill_type, status,
                    item_amount, pack_fee, delivery_fee, discount_amount, pay_amount,
                    receiver_name, receiver_phone, receiver_address, remark, promise_minutes,
                    created_at, paid_at, finished_at) VALUES
(9006, 'SG20261019100006', 1, 103, '蜜茶时光·奶茶', 'grad-3', 1, 'DELIVERY', 'COMPLETED',
 3600, 200, 200, 400, 3600,
 '张小明', '138****1111', '天津市滨海新区 数创大厦 A 座 1801', '常温少糖', 20,
 '2026-10-19 18:30:00', '2026-10-19 18:30:20', '2026-10-19 19:05:40');

INSERT INTO order_item (id, order_id, order_no, dish_id, dish_name, dish_image, spec_text, unit_price, quantity, amount) VALUES
(9107, 9006, 'SG20261019100006', 342, '生椰拿铁', 'grad-n', '中杯 / 常温 / 少冰', 2000, 1, 2000),
(9108, 9006, 'SG20261019100006', 346, '葡式蛋挞', 'grad-r', NULL, 800, 2, 1600);

INSERT INTO order_status_log (id, order_id, order_no, from_status, to_status, operator, remark, created_at) VALUES
(9235, 9006, 'SG20261019100006', NULL,          'UNPAID',     'USER',    '提交订单', '2026-10-19 18:30:00'),
(9236, 9006, 'SG20261019100006', 'UNPAID',      'WAIT_ACCEPT','USER',    '支付成功', '2026-10-19 18:30:20'),
(9237, 9006, 'SG20261019100006', 'WAIT_ACCEPT', 'PREPARING',  'MERCHANT','商家接单', '2026-10-19 18:31:00'),
(9238, 9006, 'SG20261019100006', 'PREPARING',   'WAIT_PICKUP','MERCHANT','商家出餐', '2026-10-19 18:42:10'),
(9239, 9006, 'SG20261019100006', 'WAIT_PICKUP', 'DELIVERING', 'RIDER',   '骑手已取货', '2026-10-19 18:48:30'),
(9240, 9006, 'SG20261019100006', 'DELIVERING',  'DELIVERED', 'RIDER',   '骑手已送达', '2026-10-19 19:04:10'),
(9241, 9006, 'SG20261019100006', 'DELIVERED',   'COMPLETED', 'USER',    '用户确认收货', '2026-10-19 19:05:40');

INSERT INTO payment (id, pay_no, order_id, order_no, channel, amount, status, trade_no, created_at, paid_at) VALUES
(9306, 'PAY20261019100006', 9006, 'SG20261019100006', 'ALIPAY_SANDBOX', 3600, 'SUCCESS', '2026101922006', '2026-10-19 18:30:00', '2026-10-19 18:30:20');

-- ---------------------- 骑手收入（已结算历史） ----------------------
INSERT INTO rider_income (id, rider_id, order_id, order_no, shop_name, base_fee, subsidy, penalty, income_amount, settled, settled_at, created_at) VALUES
(9401, 1, 9001, 'SG20261015100001', '星光小馆·川湘菜', 300, 200, 0, 500, 1, '2026-10-15 12:10:00', '2026-10-15 12:02:10'),
(9402, 1, 9006, 'SG20261019100006', '蜜茶时光·奶茶', 200, 200, 0, 400, 1, '2026-10-19 19:10:00', '2026-10-19 19:05:40');

-- ---------------------- 骑手消息 ----------------------
INSERT INTO rider_message (id, rider_id, type, title, content, ref_order_no, is_read, created_at) VALUES
(9501, 1, 'SYSTEM', '新人培训完成', '恭喜您完成骑手入职培训，现在可以开始接单啦！', NULL, 1, '2026-10-20 08:00:00'),
(9502, 1, 'ORDER',  '新订单待取货', '星光小馆·川湘菜 有新的待取货订单，快去接单大厅看看', 'SG20261020100004', 0, '2026-10-20 12:08:30'),
(9503, 1, 'SYSTEM', '天气提醒', '今日有降雨，请注意配送安全，平台已发放雨天补贴', NULL, 0, '2026-10-20 09:30:00');

-- ------------------------------ 运营后台账号（RBAC） ------------------------------
-- 默认密码均为 123456（MD5: e10adc3949ba59abbe56e057f20f883e）
INSERT INTO admin_user (id, username, password, name, role, phone, status) VALUES
(1, 'admin',    'e10adc3949ba59abbe56e057f20f883e', '超级管理员', 'SUPER_ADMIN', '13800000000', 1),
(2, 'operator', 'e10adc3949ba59abbe56e057f20f883e', '运营-小王',  'OPERATOR',    '13800000001', 1),
(3, 'cs',       'e10adc3949ba59abbe56e057f20f883e', '客服-小美',  'CS',          '13800000002', 1);

-- ------------------------------ 商家端账号（v1：一个账号管一家店，密码默认 123456） ------------------------------
INSERT INTO merchant_user (id, merchant_id, shop_id, username, password, name, phone, status) VALUES
(1001, 1, 101, 'm101', 'e10adc3949ba59abbe56e057f20f883e', '喜姐炸鸡·店长',  '13900000101', 1),
(1002, 1, 102, 'm102', 'e10adc3949ba59abbe56e057f20f883e', '海底捞·店长',   '13900000102', 1),
(1003, 1, 103, 'm103', 'e10adc3949ba59abbe56e057f20f883e', '蜜茶时光·店长', '13900000103', 1),
(1004, 1, 104, 'm104', 'e10adc3949ba59abbe56e057f20f883e', '老碗会·店长',   '13900000104', 1),
(1005, 1, 105, 'm105', 'e10adc3949ba59abbe56e057f20f883e', '玫瑰情缘·店长', '13900000105', 1),
(1006, 1, 106, 'm106', 'e10adc3949ba59abbe56e057f20f883e', 'CoCo都可·店长', '13900000106', 1),
(1007, 1, 107, 'm107', 'e10adc3949ba59abbe56e057f20f883e', '蜜雪冰城·店长', '13900000107', 1);

-- ------------------------------ 商家入驻申请 ------------------------------
INSERT INTO merchant_application (id, merchant_name, contact, phone, category, shop_name, address, license_no, expect_commission, status, reviewer_id, reviewed_at, created_at) VALUES
(1, '津味鲜饺子馆', '赵老板', '022-8800-2001', '美食·饺子', '津味鲜饺子馆（南开店）', '南开区长江道 88 号', '120104MA06X1X1X1', 800, 'PENDING', NULL, NULL, '2026-10-18 10:00:00'),
(2, '椰满满·椰子鸡', '孙经理', '022-8800-2002', '美食·火锅', '椰满满椰子鸡（和平店）', '和平区滨江道 66 号', '120101MA06Y2Y2Y2', 750, 'PENDING', NULL, NULL, '2026-10-19 14:30:00'),
(3, '麦香烘焙工坊', '周店长', '022-8800-2003', '美食·烘焙', '麦香烘焙工坊', '河西区友谊路 12 号', '120103MA06Z3Z3Z3', 700, 'APPROVED', 1, '2026-10-17 11:20:00', '2026-10-17 09:00:00');

-- ------------------------------ 用户投诉 ------------------------------
INSERT INTO complaint (id, order_no, user_id, user_name, type, level, content, status, handler_id, handle_note, created_at) VALUES
(1, 'SG20261016100002', 1, '星光小星', 'DELAY',    'URGENT', '骑手超时 40 分钟未送达，多次联系无果', 'OPEN',     NULL, NULL,                          '2026-10-16 10:30:00'),
(2, 'SG20261015100001', 1, '星光小星', 'QUALITY',  'NORMAL', '辣子鸡份量偏少，与图片不符',          'HANDLING', 2,    '已联系商家核实，补发 5 元无门槛券', '2026-10-15 13:00:00'),
(3, '',                1, '星光小星', 'SERVICE',  'NORMAL', '客服响应慢，希望提升服务效率',        'RESOLVED', 3,    '已优化排班，增加高峰时段客服',     '2026-10-14 20:10:00');
