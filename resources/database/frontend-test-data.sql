-- Frontend integration seed data for mall-cqupt-lqy11.
-- Run after the base DDL scripts under resources/database have created both
-- mall_coupon_cqupt_0 and mall_coupon_cqupt_1.
--
-- Product and cart tables are single tables on mall_coupon_cqupt_0.
-- Coupon templates keep sharding by shop_number:
--   shop_number=0 -> mall_coupon_cqupt_0.t_coupon_template_0
--   shop_number=1810714735922956666 -> mall_coupon_cqupt_1.t_coupon_template_15
-- User coupons keep sharding by user_id:
--   user_id=10003 -> mall_coupon_cqupt_1.t_user_coupon_19
-- Coupon settlements keep sharding by user_id:
--   user_id=10003 -> mall_coupon_cqupt_0.t_coupon_settlement_3

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS mall_coupon_cqupt_0 DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS mall_coupon_cqupt_1 DEFAULT CHARACTER SET utf8mb4;

SET @seed_password := '$2a$10$7KATYIo.cd7e6n4W7Y.o0.Db/nDxEpNPSn.uWrFAnXyyS7jeuhsHq';
SET @merchant_shop := 1810714735922956666;
SET @platform_shop := 0;
SET @platform_user := 10001;
SET @merchant_user := 10002;
SET @customer_user := 10003;
SET @cart_limit_user := 10006;

SET @rr_shop := JSON_OBJECT(
    'limitPerPerson', 3,
    'usageInstructions', 'Frontend test: shop coupon for mall pages and settlement.',
    'distributionMode', 'RECEIVE',
    'applicableMerchantScope', 'SPECIFIED',
    'applicableShopNumbers', '1810714735922956666'
);
SET @rr_shop_once := JSON_OBJECT(
    'limitPerPerson', 1,
    'usageInstructions', 'Frontend test: one coupon per person.',
    'distributionMode', 'RECEIVE',
    'applicableMerchantScope', 'SPECIFIED',
    'applicableShopNumbers', '1810714735922956666'
);
SET @rr_platform := JSON_OBJECT(
    'limitPerPerson', 2,
    'usageInstructions', 'Frontend test: platform coupon.',
    'distributionMode', 'RECEIVE',
    'applicableMerchantScope', 'ALL',
    'applicableShopNumbers', ''
);

-- ---------------------------------------------------------------------------
-- 1. Accounts: platform, merchant, customer, disabled, inactive, cart boundary.
-- Password for all accounts: Test123456
-- ---------------------------------------------------------------------------
USE mall_coupon_cqupt_0;

DELETE FROM t_user
WHERE id BETWEEN 10001 AND 10006
   OR username IN ('platform01', 'merchant01', 'customer01', 'disabled01', 'inactive01', 'cartlimit01');

INSERT INTO t_user
(id, role_type, shop_number, username, password, nickname, real_name, phone, mail, avatar_url, status, activation_status, last_login_time, create_time, update_time, del_flag)
VALUES
(10001, 0, NULL, 'platform01', @seed_password, '平台测试账号', '平台管理员', '13900001001', 'platform01@test.local', 'https://dummyimage.com/160x160/2563eb/ffffff&text=P', 0, 1, NULL, '2026-04-01 09:00:00', '2026-04-01 09:00:00', 0),
(10002, 1, '1810714735922956666', 'merchant01', @seed_password, '邮惠测试商家', '商家测试员', '13900001002', 'merchant01@test.local', 'https://dummyimage.com/160x160/059669/ffffff&text=M', 0, 1, NULL, '2026-04-01 09:05:00', '2026-04-01 09:05:00', 0),
(10003, 2, NULL, 'customer01', @seed_password, '前端测试用户', '普通测试员', '13900001003', 'customer01@test.local', 'https://dummyimage.com/160x160/f97316/ffffff&text=C', 0, 1, NULL, '2026-04-01 09:10:00', '2026-04-01 09:10:00', 0),
(10004, 2, NULL, 'disabled01', @seed_password, '禁用测试用户', '禁用账号', '13900001004', 'disabled01@test.local', 'https://dummyimage.com/160x160/64748b/ffffff&text=D', 1, 1, NULL, '2026-04-01 09:15:00', '2026-04-01 09:15:00', 0),
(10005, 2, NULL, 'inactive01', @seed_password, '未激活测试用户', '未激活账号', '13900001005', 'inactive01@test.local', 'https://dummyimage.com/160x160/64748b/ffffff&text=I', 0, 0, NULL, '2026-04-01 09:20:00', '2026-04-01 09:20:00', 0),
(10006, 2, NULL, 'cartlimit01', @seed_password, '购物车边界用户', '购物车边界', '13900001006', 'cartlimit01@test.local', 'https://dummyimage.com/160x160/7c3aed/ffffff&text=50', 0, 1, NULL, '2026-04-01 09:25:00', '2026-04-01 09:25:00', 0)
ON DUPLICATE KEY UPDATE
    role_type = VALUES(role_type),
    shop_number = VALUES(shop_number),
    password = VALUES(password),
    nickname = VALUES(nickname),
    real_name = VALUES(real_name),
    phone = VALUES(phone),
    mail = VALUES(mail),
    avatar_url = VALUES(avatar_url),
    status = VALUES(status),
    activation_status = VALUES(activation_status),
    update_time = VALUES(update_time),
    del_flag = VALUES(del_flag);

-- ---------------------------------------------------------------------------
-- 2. Product base data: category tree, attributes, goods, images, attributes.
-- ---------------------------------------------------------------------------
DELETE FROM t_cart WHERE user_id IN (10003, 10006) OR id BETWEEN 820001 AND 825049;
DELETE FROM t_goods_attribute_value WHERE goods_id BETWEEN 800001 AND 800010 OR goods_id BETWEEN 801001 AND 801049;
DELETE FROM t_goods_image WHERE goods_id BETWEEN 800001 AND 800010 OR goods_id BETWEEN 801001 AND 801049;
DELETE FROM t_goods WHERE id BETWEEN 800001 AND 800010 OR id BETWEEN 801001 AND 801049;
DELETE FROM t_goods_attribute WHERE id BETWEEN 7001 AND 7004;
DELETE FROM t_goods_category WHERE id BETWEEN 5001 AND 5099;

INSERT INTO t_goods_category
(id, parent_id, name, icon, sort_order, level, status, create_time, update_time, del_flag)
VALUES
(5001, 0, '数码', 'https://dummyimage.com/96x96/2563eb/ffffff&text=Digital', 10, 1, 0, '2026-04-01 10:00:00', '2026-04-01 10:00:00', 0),
(5002, 5001, '智能设备', 'https://dummyimage.com/96x96/2563eb/ffffff&text=Device', 11, 2, 0, '2026-04-01 10:01:00', '2026-04-01 10:01:00', 0),
(5003, 5001, '数码配件', 'https://dummyimage.com/96x96/2563eb/ffffff&text=Accessory', 12, 2, 0, '2026-04-01 10:02:00', '2026-04-01 10:02:00', 0),
(5010, 0, '食品', 'https://dummyimage.com/96x96/f97316/ffffff&text=Food', 20, 1, 0, '2026-04-01 10:03:00', '2026-04-01 10:03:00', 0),
(5011, 5010, '休闲零食', 'https://dummyimage.com/96x96/f97316/ffffff&text=Snack', 21, 2, 0, '2026-04-01 10:04:00', '2026-04-01 10:04:00', 0),
(5012, 5010, '饮品冲调', 'https://dummyimage.com/96x96/f97316/ffffff&text=Drink', 22, 2, 0, '2026-04-01 10:05:00', '2026-04-01 10:05:00', 0),
(5020, 0, '文创', 'https://dummyimage.com/96x96/7c3aed/ffffff&text=Gift', 30, 1, 0, '2026-04-01 10:06:00', '2026-04-01 10:06:00', 0),
(5021, 5020, '校园文具', 'https://dummyimage.com/96x96/7c3aed/ffffff&text=Pen', 31, 2, 0, '2026-04-01 10:07:00', '2026-04-01 10:07:00', 0),
(5022, 5020, '纪念周边', 'https://dummyimage.com/96x96/7c3aed/ffffff&text=Souvenir', 32, 2, 0, '2026-04-01 10:08:00', '2026-04-01 10:08:00', 0),
(5030, 0, '生活用品', 'https://dummyimage.com/96x96/059669/ffffff&text=Life', 40, 1, 0, '2026-04-01 10:09:00', '2026-04-01 10:09:00', 0),
(5031, 5030, '清洁护理', 'https://dummyimage.com/96x96/059669/ffffff&text=Clean', 41, 2, 0, '2026-04-01 10:10:00', '2026-04-01 10:10:00', 0),
(5032, 5030, '收纳整理', 'https://dummyimage.com/96x96/059669/ffffff&text=Storage', 42, 2, 0, '2026-04-01 10:11:00', '2026-04-01 10:11:00', 0),
(5099, 5030, '禁用分类', 'https://dummyimage.com/96x96/64748b/ffffff&text=Disabled', 99, 2, 1, '2026-04-01 10:12:00', '2026-04-01 10:12:00', 0)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    name = VALUES(name),
    icon = VALUES(icon),
    sort_order = VALUES(sort_order),
    level = VALUES(level),
    status = VALUES(status),
    update_time = VALUES(update_time),
    del_flag = VALUES(del_flag);

INSERT INTO t_goods_attribute
(id, name, input_type, `values`, sort_order, status, create_time, update_time, del_flag)
VALUES
(7001, '颜色', 1, '黑色,白色,红色,蓝色', 10, 0, '2026-04-01 10:20:00', '2026-04-01 10:20:00', 0),
(7002, '规格', 1, '标准版,升级版,家庭装,礼盒装', 20, 0, '2026-04-01 10:21:00', '2026-04-01 10:21:00', 0),
(7003, '保质期', 0, NULL, 30, 0, '2026-04-01 10:22:00', '2026-04-01 10:22:00', 0),
(7004, '材质', 2, 'ABS,硅胶,纸质,棉麻,不锈钢', 40, 0, '2026-04-01 10:23:00', '2026-04-01 10:23:00', 0)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    input_type = VALUES(input_type),
    `values` = VALUES(`values`),
    sort_order = VALUES(sort_order),
    status = VALUES(status),
    update_time = VALUES(update_time),
    del_flag = VALUES(del_flag);

INSERT INTO t_goods
(id, shop_number, category_id, name, description, main_image, price, original_price, stock, sales, unit, status, sort_order, create_time, update_time, del_flag)
VALUES
(800001, @merchant_shop, 5002, '[CQUPT-GOODS-001] 邮惠智能手环', '用于商城首页、详情页、加购、结算和商品专属券匹配。', 'https://dummyimage.com/800x800/2563eb/ffffff&text=CQUPT-GOODS-001', 199.00, 259.00, 50, 128, '件', 1, 10, '2026-04-01 11:00:00', '2026-04-01 11:00:00', 0),
(800002, @merchant_shop, 5003, 'Type-C 快充数据线', '用于购物车未选中项、商品列表分页和搜索。', 'https://dummyimage.com/800x800/0f766e/ffffff&text=Cable', 39.90, 59.90, 88, 321, '条', 1, 20, '2026-04-01 11:05:00', '2026-04-01 11:05:00', 0),
(800003, @merchant_shop, 5011, '校园能量坚果包', '库存为0，用于售罄和不可购买提示。', 'https://dummyimage.com/800x800/f97316/ffffff&text=Sold+Out', 12.90, 18.90, 0, 490, '包', 1, 30, '2026-04-01 11:10:00', '2026-04-01 11:10:00', 0),
(800004, @merchant_shop, 5021, '低价测试贴纸', '价格0.01、库存1，用于边界金额和低库存提示。', 'https://dummyimage.com/800x800/7c3aed/ffffff&text=0.01', 0.01, 1.00, 1, 6, '张', 1, 40, '2026-04-01 11:15:00', '2026-04-01 11:15:00', 0),
(800005, @merchant_shop, 5032, '高价大库存收纳箱', '价格999999.99、库存999999，用于金额和数量边界。', 'https://dummyimage.com/800x800/059669/ffffff&text=999999.99', 999999.99, 1099999.99, 999999, 1, '套', 1, 50, '2026-04-01 11:20:00', '2026-04-01 11:20:00', 0),
(800006, @merchant_shop, 5012, '下架饮品礼盒', '商家后台可见，用户商城不可见。', 'https://dummyimage.com/800x800/64748b/ffffff&text=Off+Shelf', 88.00, 99.00, 20, 0, '盒', 0, 60, '2026-04-01 11:25:00', '2026-04-01 11:25:00', 0),
(800007, @merchant_shop, 5022, '违规下架纪念品', '用于商品管理违规状态标签。', 'https://dummyimage.com/800x800/dc2626/ffffff&text=Violation', 66.00, 88.00, 10, 3, '件', 2, 70, '2026-04-01 11:30:00', '2026-04-01 11:30:00', 0),
(800008, @merchant_shop, 5031, '无图兜底清洁套装', 'main_image为空，用于前端图片兜底展示。', NULL, 29.90, 39.90, 34, 41, '套', 1, 80, '2026-04-01 11:35:00', '2026-04-01 11:35:00', 0),
(800009, @merchant_shop, 5099, '禁用分类测试商品', '挂在禁用分类下，用于创建和编辑校验。', 'https://dummyimage.com/800x800/64748b/ffffff&text=Disabled+Category', 19.90, 29.90, 12, 5, '件', 1, 90, '2026-04-01 11:40:00', '2026-04-01 11:40:00', 0),
(800010, @merchant_shop, 5002, '缓存缺失测试商品', '不预热Redis缓存，用于购物车商品已下架兜底。', 'https://dummyimage.com/800x800/111827/ffffff&text=No+Cache', 9.90, 19.90, 10, 0, '件', 1, 100, '2026-04-01 11:45:00', '2026-04-01 11:45:00', 0)
ON DUPLICATE KEY UPDATE
    shop_number = VALUES(shop_number),
    category_id = VALUES(category_id),
    name = VALUES(name),
    description = VALUES(description),
    main_image = VALUES(main_image),
    price = VALUES(price),
    original_price = VALUES(original_price),
    stock = VALUES(stock),
    sales = VALUES(sales),
    unit = VALUES(unit),
    status = VALUES(status),
    sort_order = VALUES(sort_order),
    update_time = VALUES(update_time),
    del_flag = VALUES(del_flag);

INSERT INTO t_goods_image
(id, shop_number, goods_id, image_url, sort_order, create_time, del_flag)
VALUES
(810001, @merchant_shop, 800001, 'https://dummyimage.com/800x800/2563eb/ffffff&text=Band+Main', 1, '2026-04-01 12:00:00', 0),
(810002, @merchant_shop, 800001, 'https://dummyimage.com/800x800/1d4ed8/ffffff&text=Band+Detail', 2, '2026-04-01 12:01:00', 0),
(810003, @merchant_shop, 800001, 'https://dummyimage.com/800x800/1e40af/ffffff&text=Band+Package', 3, '2026-04-01 12:02:00', 0),
(810004, @merchant_shop, 800002, 'https://dummyimage.com/800x800/0f766e/ffffff&text=Cable+1', 1, '2026-04-01 12:03:00', 0),
(810005, @merchant_shop, 800002, 'https://dummyimage.com/800x800/115e59/ffffff&text=Cable+2', 2, '2026-04-01 12:04:00', 0),
(810006, @merchant_shop, 800003, 'https://dummyimage.com/800x800/f97316/ffffff&text=Nuts', 1, '2026-04-01 12:05:00', 0),
(810007, @merchant_shop, 800004, 'https://dummyimage.com/800x800/7c3aed/ffffff&text=Sticker', 1, '2026-04-01 12:06:00', 0),
(810008, @merchant_shop, 800005, 'https://dummyimage.com/800x800/059669/ffffff&text=Storage', 1, '2026-04-01 12:07:00', 0),
(810009, @merchant_shop, 800006, 'https://dummyimage.com/800x800/64748b/ffffff&text=Gift', 1, '2026-04-01 12:08:00', 0),
(810010, @merchant_shop, 800007, 'https://dummyimage.com/800x800/dc2626/ffffff&text=Blocked', 1, '2026-04-01 12:09:00', 0),
(810011, @merchant_shop, 800009, 'https://dummyimage.com/800x800/64748b/ffffff&text=Disabled', 1, '2026-04-01 12:10:00', 0)
ON DUPLICATE KEY UPDATE
    shop_number = VALUES(shop_number),
    goods_id = VALUES(goods_id),
    image_url = VALUES(image_url),
    sort_order = VALUES(sort_order),
    del_flag = VALUES(del_flag);

INSERT INTO t_goods_attribute_value
(id, shop_number, goods_id, attribute_id, attribute_value, create_time, del_flag)
VALUES
(811001, @merchant_shop, 800001, 7001, '黑色', '2026-04-01 12:20:00', 0),
(811002, @merchant_shop, 800001, 7002, '升级版', '2026-04-01 12:20:00', 0),
(811003, @merchant_shop, 800001, 7004, 'ABS,硅胶', '2026-04-01 12:20:00', 0),
(811004, @merchant_shop, 800002, 7001, '白色', '2026-04-01 12:21:00', 0),
(811005, @merchant_shop, 800002, 7002, '标准版', '2026-04-01 12:21:00', 0),
(811006, @merchant_shop, 800003, 7003, '9个月', '2026-04-01 12:22:00', 0),
(811007, @merchant_shop, 800004, 7001, '蓝色', '2026-04-01 12:23:00', 0),
(811008, @merchant_shop, 800005, 7004, '棉麻,不锈钢', '2026-04-01 12:24:00', 0),
(811009, @merchant_shop, 800008, 7002, '家庭装', '2026-04-01 12:25:00', 0),
(811010, @merchant_shop, 800009, 7001, '红色', '2026-04-01 12:26:00', 0)
ON DUPLICATE KEY UPDATE
    shop_number = VALUES(shop_number),
    goods_id = VALUES(goods_id),
    attribute_id = VALUES(attribute_id),
    attribute_value = VALUES(attribute_value),
    del_flag = VALUES(del_flag);

-- Boundary user with 49 distinct cart goods.
-- Keep this as plain SQL so IDEs that do not understand DELIMITER can execute it.
DROP TEMPORARY TABLE IF EXISTS seed_frontend_seq;
CREATE TEMPORARY TABLE seed_frontend_seq (i INT PRIMARY KEY);
INSERT INTO seed_frontend_seq (i) VALUES
(1),(2),(3),(4),(5),(6),(7),(8),(9),(10),
(11),(12),(13),(14),(15),(16),(17),(18),(19),(20),
(21),(22),(23),(24),(25),(26),(27),(28),(29),(30),
(31),(32),(33),(34),(35),(36),(37),(38),(39),(40),
(41),(42),(43),(44),(45),(46),(47),(48),(49);

INSERT INTO t_goods
(id, shop_number, category_id, name, description, main_image, price, original_price, stock, sales, unit, status, sort_order, create_time, update_time, del_flag)
SELECT
    801000 + i,
    @merchant_shop,
    5003,
    CONCAT('购物车边界商品 ', LPAD(i, 2, '0')),
    '用于购物车接近50种商品边界验证。',
    CONCAT('https://dummyimage.com/800x800/334155/ffffff&text=Cart+', LPAD(i, 2, '0')),
    10.00 + (i / 10),
    12.00 + (i / 10),
    100 + i,
    i,
    '件',
    1,
    2000 + i,
    '2026-04-01 13:00:00',
    '2026-04-01 13:00:00',
    0
FROM seed_frontend_seq
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    main_image = VALUES(main_image),
    price = VALUES(price),
    original_price = VALUES(original_price),
    stock = VALUES(stock),
    sales = VALUES(sales),
    status = VALUES(status),
    sort_order = VALUES(sort_order),
    update_time = VALUES(update_time),
    del_flag = VALUES(del_flag);

INSERT INTO t_cart
(id, user_id, goods_id, shop_number, quantity, selected, create_time, update_time, del_flag)
SELECT
    825000 + i,
    @cart_limit_user,
    801000 + i,
    @merchant_shop,
    1,
    IF(i % 2 = 0, 0, 1),
    DATE_ADD('2026-04-01 13:00:00', INTERVAL i MINUTE),
    DATE_ADD('2026-04-01 13:00:00', INTERVAL i MINUTE),
    0
FROM seed_frontend_seq
ON DUPLICATE KEY UPDATE
    quantity = VALUES(quantity),
    selected = VALUES(selected),
    update_time = VALUES(update_time),
    del_flag = VALUES(del_flag);

DROP TEMPORARY TABLE IF EXISTS seed_frontend_seq;

-- ---------------------------------------------------------------------------
-- 3. Cart data for customer01.
-- ---------------------------------------------------------------------------
INSERT INTO t_cart
(id, user_id, goods_id, shop_number, quantity, selected, create_time, update_time, del_flag)
VALUES
(820001, @customer_user, 800001, @merchant_shop, 1, 1, '2026-04-20 09:00:00', '2026-04-20 09:00:00', 0),
(820002, @customer_user, 800002, @merchant_shop, 2, 0, '2026-04-20 09:05:00', '2026-04-20 09:05:00', 0),
(820003, @customer_user, 800003, @merchant_shop, 2, 1, '2026-04-20 09:10:00', '2026-04-20 09:10:00', 0),
(820004, @customer_user, 800005, @merchant_shop, 999, 0, '2026-04-20 09:15:00', '2026-04-20 09:15:00', 0),
(820005, @customer_user, 800010, @merchant_shop, 1, 1, '2026-04-20 09:20:00', '2026-04-20 09:20:00', 0),
(820006, @customer_user, 800004, @merchant_shop, 1, 1, '2026-04-20 09:25:00', '2026-04-20 09:25:00', 1)
ON DUPLICATE KEY UPDATE
    quantity = VALUES(quantity),
    selected = VALUES(selected),
    update_time = VALUES(update_time),
    del_flag = VALUES(del_flag);

-- ---------------------------------------------------------------------------
-- 4. Merchant coupon templates: ds_1.t_coupon_template_15.
-- ---------------------------------------------------------------------------
USE mall_coupon_cqupt_1;

DELETE FROM t_coupon_template_log_15 WHERE coupon_template_id BETWEEN 910000000000001 AND 910000000000010;
DELETE FROM t_coupon_template_15 WHERE id BETWEEN 910000000000001 AND 910000000000010;

INSERT INTO t_coupon_template_15
(id, name, shop_number, source, target, goods, type, valid_start_time, valid_end_time, stock, receive_rule, consume_rule, status, create_time, update_time, del_flag)
VALUES
(910000000000001, '店铺立减20元', @merchant_shop, 0, 1, NULL, 0, '2026-04-01 00:00:00', '2026-12-31 23:59:59', 100, @rr_shop, JSON_OBJECT('termsOfUse', 0, 'thresholdAmount', 0, 'maximumDiscountAmount', 20.00, 'maxDiscountAmount', 20.00, 'discountAmount', 20.00, 'explanationOfUnmetConditions', '当前订单暂不可用'), 0, '2026-04-01 14:00:00', '2026-04-01 14:00:00', 0),
(910000000000002, '店铺满199减30', @merchant_shop, 0, 1, NULL, 1, '2026-04-01 00:00:00', '2026-12-31 23:59:59', 1, @rr_shop_once, JSON_OBJECT('termsOfUse', 199.00, 'thresholdAmount', 199.00, 'maximumDiscountAmount', 30.00, 'maxDiscountAmount', 30.00, 'discountAmount', 30.00, 'explanationOfUnmetConditions', '订单未满199元'), 0, '2026-04-01 14:05:00', '2026-04-01 14:05:00', 0),
(910000000000003, '店铺9折最高80', @merchant_shop, 0, 1, NULL, 2, '2026-04-01 00:00:00', '2026-12-31 23:59:59', 50, @rr_shop, JSON_OBJECT('termsOfUse', 100.00, 'thresholdAmount', 100.00, 'maximumDiscountAmount', 80.00, 'maxDiscountAmount', 80.00, 'discountAmount', 80.00, 'discountRate', 9.00, 'explanationOfUnmetConditions', '订单未满100元'), 0, '2026-04-01 14:10:00', '2026-04-01 14:10:00', 0),
(910000000000004, '商品CQUPT-GOODS-001满199减50', @merchant_shop, 0, 0, 'CQUPT-GOODS-001', 1, '2026-04-01 00:00:00', '2026-12-31 23:59:59', 20, @rr_shop, JSON_OBJECT('termsOfUse', 199.00, 'thresholdAmount', 199.00, 'maximumDiscountAmount', 50.00, 'maxDiscountAmount', 50.00, 'discountAmount', 50.00, 'explanationOfUnmetConditions', '指定商品未满199元或商品编码不匹配'), 0, '2026-04-01 14:15:00', '2026-04-01 14:15:00', 0),
(910000000000005, '购物车商品800001立减15', @merchant_shop, 0, 0, '800001', 0, '2026-04-01 00:00:00', '2026-12-31 23:59:59', 20, @rr_shop, JSON_OBJECT('termsOfUse', 0, 'thresholdAmount', 0, 'maximumDiscountAmount', 15.00, 'maxDiscountAmount', 15.00, 'discountAmount', 15.00, 'explanationOfUnmetConditions', '购物车商品编码不匹配'), 0, '2026-04-01 14:20:00', '2026-04-01 14:20:00', 0),
(910000000000006, '未开始预约券满99减12', @merchant_shop, 0, 1, NULL, 1, '2026-05-01 10:00:00', '2026-12-31 23:59:59', 100, @rr_shop, JSON_OBJECT('termsOfUse', 99.00, 'thresholdAmount', 99.00, 'maximumDiscountAmount', 12.00, 'maxDiscountAmount', 12.00, 'discountAmount', 12.00, 'explanationOfUnmetConditions', '活动尚未开始或未满99元'), 0, '2026-04-01 14:25:00', '2026-04-01 14:25:00', 0),
(910000000000007, '已过期满59减5', @merchant_shop, 0, 1, NULL, 1, '2026-01-01 00:00:00', '2026-03-31 23:59:59', 100, @rr_shop, JSON_OBJECT('termsOfUse', 59.00, 'thresholdAmount', 59.00, 'maximumDiscountAmount', 5.00, 'maxDiscountAmount', 5.00, 'discountAmount', 5.00, 'explanationOfUnmetConditions', '优惠券已过期'), 0, '2026-04-01 14:30:00', '2026-04-01 14:30:00', 0),
(910000000000008, '库存为0满减券', @merchant_shop, 0, 1, NULL, 1, '2026-04-01 00:00:00', '2026-12-31 23:59:59', 0, @rr_shop, JSON_OBJECT('termsOfUse', 59.00, 'thresholdAmount', 59.00, 'maximumDiscountAmount', 6.00, 'maxDiscountAmount', 6.00, 'discountAmount', 6.00, 'explanationOfUnmetConditions', '优惠券库存不足'), 0, '2026-04-01 14:35:00', '2026-04-01 14:35:00', 0),
(910000000000009, '已结束店铺券', @merchant_shop, 0, 1, NULL, 0, '2026-04-01 00:00:00', '2026-12-31 23:59:59', 100, @rr_shop, JSON_OBJECT('termsOfUse', 0, 'thresholdAmount', 0, 'maximumDiscountAmount', 8.00, 'maxDiscountAmount', 8.00, 'discountAmount', 8.00, 'explanationOfUnmetConditions', '优惠券已结束'), 1, '2026-04-01 14:40:00', '2026-04-01 14:40:00', 0),
(910000000000010, '大库存满9999减500', @merchant_shop, 0, 1, NULL, 1, '2026-04-01 00:00:00', '2026-12-31 23:59:59', 999999, @rr_shop, JSON_OBJECT('termsOfUse', 9999.00, 'thresholdAmount', 9999.00, 'maximumDiscountAmount', 500.00, 'maxDiscountAmount', 500.00, 'discountAmount', 500.00, 'explanationOfUnmetConditions', '订单未满9999元'), 0, '2026-04-01 14:45:00', '2026-04-01 14:45:00', 0)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    shop_number = VALUES(shop_number),
    source = VALUES(source),
    target = VALUES(target),
    goods = VALUES(goods),
    type = VALUES(type),
    valid_start_time = VALUES(valid_start_time),
    valid_end_time = VALUES(valid_end_time),
    stock = VALUES(stock),
    receive_rule = VALUES(receive_rule),
    consume_rule = VALUES(consume_rule),
    status = VALUES(status),
    update_time = VALUES(update_time),
    del_flag = VALUES(del_flag);

INSERT INTO t_coupon_template_log_15
(id, shop_number, coupon_template_id, operator_id, operation_log, original_data, modified_data, create_time)
VALUES
(915000000000001, @merchant_shop, 910000000000001, @merchant_user, 'seed create shop fixed coupon', NULL, 'frontend-test-data.sql', '2026-04-01 15:00:00'),
(915000000000002, @merchant_shop, 910000000000002, @merchant_user, 'seed create shop threshold coupon', NULL, 'frontend-test-data.sql', '2026-04-01 15:01:00'),
(915000000000003, @merchant_shop, 910000000000003, @merchant_user, 'seed create shop discount coupon', NULL, 'frontend-test-data.sql', '2026-04-01 15:02:00'),
(915000000000004, @merchant_shop, 910000000000004, @merchant_user, 'seed create product coupon', NULL, 'frontend-test-data.sql', '2026-04-01 15:03:00'),
(915000000000005, @merchant_shop, 910000000000005, @merchant_user, 'seed create cart product coupon', NULL, 'frontend-test-data.sql', '2026-04-01 15:04:00')
ON DUPLICATE KEY UPDATE
    operation_log = VALUES(operation_log),
    modified_data = VALUES(modified_data),
    create_time = VALUES(create_time);

-- ---------------------------------------------------------------------------
-- 5. Platform coupon templates: ds_0.t_coupon_template_0.
-- ---------------------------------------------------------------------------
USE mall_coupon_cqupt_0;

DELETE FROM t_coupon_template_log_0 WHERE coupon_template_id BETWEEN 910000000000101 AND 910000000000104;
DELETE FROM t_coupon_template_0 WHERE id BETWEEN 910000000000101 AND 910000000000104;

INSERT INTO t_coupon_template_0
(id, name, shop_number, source, target, goods, type, valid_start_time, valid_end_time, stock, receive_rule, consume_rule, status, create_time, update_time, del_flag)
VALUES
(910000000000101, '平台全场满299减40', @platform_shop, 1, 1, NULL, 1, '2026-04-01 00:00:00', '2026-12-31 23:59:59', 200, @rr_platform, JSON_OBJECT('termsOfUse', 299.00, 'thresholdAmount', 299.00, 'maximumDiscountAmount', 40.00, 'maxDiscountAmount', 40.00, 'discountAmount', 40.00, 'explanationOfUnmetConditions', '订单未满299元'), 0, '2026-04-01 15:20:00', '2026-04-01 15:20:00', 0),
(910000000000102, '平台立减8元', @platform_shop, 1, 1, NULL, 0, '2026-04-01 00:00:00', '2026-12-31 23:59:59', 500, @rr_platform, JSON_OBJECT('termsOfUse', 0, 'thresholdAmount', 0, 'maximumDiscountAmount', 8.00, 'maxDiscountAmount', 8.00, 'discountAmount', 8.00, 'explanationOfUnmetConditions', '当前订单暂不可用'), 0, '2026-04-01 15:25:00', '2026-04-01 15:25:00', 0),
(910000000000103, '平台9.5折最高60', @platform_shop, 1, 1, NULL, 2, '2026-04-01 00:00:00', '2026-12-31 23:59:59', 100, @rr_platform, JSON_OBJECT('termsOfUse', 100.00, 'thresholdAmount', 100.00, 'maximumDiscountAmount', 60.00, 'maxDiscountAmount', 60.00, 'discountAmount', 60.00, 'discountRate', 9.50, 'explanationOfUnmetConditions', '订单未满100元'), 0, '2026-04-01 15:30:00', '2026-04-01 15:30:00', 0),
(910000000000104, '平台已过期满99减10', @platform_shop, 1, 1, NULL, 1, '2026-01-01 00:00:00', '2026-03-31 23:59:59', 100, @rr_platform, JSON_OBJECT('termsOfUse', 99.00, 'thresholdAmount', 99.00, 'maximumDiscountAmount', 10.00, 'maxDiscountAmount', 10.00, 'discountAmount', 10.00, 'explanationOfUnmetConditions', '优惠券已过期'), 0, '2026-04-01 15:35:00', '2026-04-01 15:35:00', 0)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    shop_number = VALUES(shop_number),
    source = VALUES(source),
    target = VALUES(target),
    goods = VALUES(goods),
    type = VALUES(type),
    valid_start_time = VALUES(valid_start_time),
    valid_end_time = VALUES(valid_end_time),
    stock = VALUES(stock),
    receive_rule = VALUES(receive_rule),
    consume_rule = VALUES(consume_rule),
    status = VALUES(status),
    update_time = VALUES(update_time),
    del_flag = VALUES(del_flag);

INSERT INTO t_coupon_template_log_0
(id, shop_number, coupon_template_id, operator_id, operation_log, original_data, modified_data, create_time)
VALUES
(915000000000101, @platform_shop, 910000000000101, @platform_user, 'seed create platform threshold coupon', NULL, 'frontend-test-data.sql', '2026-04-01 15:40:00'),
(915000000000102, @platform_shop, 910000000000102, @platform_user, 'seed create platform fixed coupon', NULL, 'frontend-test-data.sql', '2026-04-01 15:41:00'),
(915000000000103, @platform_shop, 910000000000103, @platform_user, 'seed create platform discount coupon', NULL, 'frontend-test-data.sql', '2026-04-01 15:42:00')
ON DUPLICATE KEY UPDATE
    operation_log = VALUES(operation_log),
    modified_data = VALUES(modified_data),
    create_time = VALUES(create_time);

-- ---------------------------------------------------------------------------
-- 6. User coupons: customer01 routes to ds_1.t_user_coupon_19.
-- ---------------------------------------------------------------------------
USE mall_coupon_cqupt_1;

DELETE FROM t_user_coupon_19 WHERE user_id = 10003 OR id BETWEEN 920000000000001 AND 920000000000009;

INSERT INTO t_user_coupon_19
(id, user_id, coupon_template_id, receive_time, receive_count, valid_start_time, valid_end_time, use_time, source, status, create_time, update_time, del_flag)
VALUES
(920000000000001, @customer_user, 910000000000002, '2026-04-20 10:00:00', 1, '2026-04-01 00:00:00', '2026-12-31 23:59:59', NULL, 2, 0, '2026-04-20 10:00:00', '2026-04-20 10:00:00', 0),
(920000000000002, @customer_user, 910000000000004, '2026-04-20 10:05:00', 1, '2026-04-01 00:00:00', '2026-12-31 23:59:59', NULL, 2, 0, '2026-04-20 10:05:00', '2026-04-20 10:05:00', 0),
(920000000000003, @customer_user, 910000000000005, '2026-04-20 10:10:00', 1, '2026-04-01 00:00:00', '2026-12-31 23:59:59', NULL, 2, 0, '2026-04-20 10:10:00', '2026-04-20 10:10:00', 0),
(920000000000004, @customer_user, 910000000000001, '2026-04-20 10:15:00', 1, '2026-04-01 00:00:00', '2026-12-31 23:59:59', NULL, 2, 1, '2026-04-20 10:15:00', '2026-04-20 10:15:00', 0),
(920000000000005, @customer_user, 910000000000003, '2026-04-20 10:20:00', 1, '2026-04-01 00:00:00', '2026-12-31 23:59:59', '2026-04-21 09:00:00', 2, 2, '2026-04-20 10:20:00', '2026-04-21 09:00:00', 0),
(920000000000006, @customer_user, 910000000000007, '2026-03-01 10:25:00', 1, '2026-01-01 00:00:00', '2026-03-31 23:59:59', NULL, 2, 3, '2026-03-01 10:25:00', '2026-04-01 00:00:00', 0),
(920000000000007, @customer_user, 910000000000009, '2026-04-20 10:30:00', 1, '2026-04-01 00:00:00', '2026-12-31 23:59:59', NULL, 2, 4, '2026-04-20 10:30:00', '2026-04-20 10:30:00', 0),
(920000000000008, @customer_user, 910000000000010, '2026-04-20 10:35:00', 1, '2026-04-01 00:00:00', '2026-12-31 23:59:59', NULL, 2, 0, '2026-04-20 10:35:00', '2026-04-20 10:35:00', 0),
(920000000000009, @customer_user, 910000000000102, '2026-04-20 10:40:00', 1, '2026-04-01 00:00:00', '2026-12-31 23:59:59', NULL, 1, 0, '2026-04-20 10:40:00', '2026-04-20 10:40:00', 0)
ON DUPLICATE KEY UPDATE
    coupon_template_id = VALUES(coupon_template_id),
    receive_time = VALUES(receive_time),
    receive_count = VALUES(receive_count),
    valid_start_time = VALUES(valid_start_time),
    valid_end_time = VALUES(valid_end_time),
    use_time = VALUES(use_time),
    source = VALUES(source),
    status = VALUES(status),
    update_time = VALUES(update_time),
    del_flag = VALUES(del_flag);

-- ---------------------------------------------------------------------------
-- 7. Coupon settlement records: customer01 routes to ds_0.t_coupon_settlement_3.
-- ---------------------------------------------------------------------------
USE mall_coupon_cqupt_0;

CREATE TABLE IF NOT EXISTS t_coupon_settlement_3
(
    id bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    order_id bigint(20) DEFAULT NULL COMMENT 'order id',
    user_id bigint(20) DEFAULT NULL COMMENT 'user id',
    coupon_id bigint(20) DEFAULT NULL COMMENT 'coupon id',
    status int(11) DEFAULT NULL COMMENT '0 locked, 1 canceled, 2 paid, 3 refunded',
    create_time datetime DEFAULT NULL COMMENT 'create time',
    update_time datetime DEFAULT NULL COMMENT 'update time',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='coupon settlement table';

SET @order_id_column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'mall_coupon_cqupt_0'
      AND TABLE_NAME = 't_coupon_settlement_3'
      AND COLUMN_NAME = 'order_id'
);
SET @seed_alter_settlement_order_id := IF(
    @order_id_column_exists = 0,
    'ALTER TABLE `mall_coupon_cqupt_0`.`t_coupon_settlement_3` ADD COLUMN `order_id` bigint(20) DEFAULT NULL COMMENT ''order id'' AFTER `id`',
    'SELECT 1'
);
PREPARE seed_stmt FROM @seed_alter_settlement_order_id;
EXECUTE seed_stmt;
DEALLOCATE PREPARE seed_stmt;

DELETE FROM t_coupon_settlement_3 WHERE user_id = 10003 OR id BETWEEN 930000000000001 AND 930000000000003;

INSERT INTO t_coupon_settlement_3
(id, order_id, user_id, coupon_id, status, create_time, update_time)
VALUES
(930000000000001, 202604270001, @customer_user, 920000000000004, 0, '2026-04-22 11:00:00', '2026-04-22 11:00:00'),
(930000000000002, 202604270002, @customer_user, 920000000000005, 2, '2026-04-22 12:00:00', '2026-04-22 12:10:00'),
(930000000000003, 202604270003, @customer_user, 920000000000005, 3, '2026-04-23 12:00:00', '2026-04-23 12:30:00')
ON DUPLICATE KEY UPDATE
    order_id = VALUES(order_id),
    coupon_id = VALUES(coupon_id),
    status = VALUES(status),
    update_time = VALUES(update_time);

-- ---------------------------------------------------------------------------
-- 8. Coupon reminders: single table on ds_0.
-- Bitmap examples:
--   8193 = 5-minute email + 10-minute SMS
--   8225 = 5-minute email + 30-minute email + 10-minute SMS
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_coupon_template_remind
(
    user_id bigint(20) NOT NULL COMMENT 'user id',
    coupon_template_id bigint(20) NOT NULL COMMENT 'coupon template id',
    information bigint(20) DEFAULT NULL COMMENT 'remind bitmap',
    shop_number bigint(20) DEFAULT NULL COMMENT 'shop number',
    start_time datetime DEFAULT NULL COMMENT 'coupon start time',
    PRIMARY KEY (user_id, coupon_template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='coupon remind table';

DELETE FROM t_coupon_template_remind WHERE user_id = 10003;

INSERT INTO t_coupon_template_remind
(user_id, coupon_template_id, information, shop_number, start_time)
VALUES
(@customer_user, 910000000000006, 8193, @merchant_shop, '2026-05-01 10:00:00'),
(@customer_user, 910000000000010, 8225, @merchant_shop, '2026-04-01 00:00:00')
ON DUPLICATE KEY UPDATE
    information = VALUES(information),
    shop_number = VALUES(shop_number),
    start_time = VALUES(start_time);

-- ---------------------------------------------------------------------------
-- 9. Coupon push tasks: single table on ds_0.
-- ---------------------------------------------------------------------------
DELETE FROM t_coupon_task WHERE id BETWEEN 940000000000001 AND 940000000000005;

INSERT INTO t_coupon_task
(id, shop_number, batch_id, task_name, file_address, fail_file_address, send_num, notify_type, coupon_template_id, send_type, send_time, status, completion_time, create_time, operator_id, update_time, del_flag)
VALUES
(940000000000001, @merchant_shop, 202604270001, '待执行-满199减30推送', 'E:/IdeaProjects/mall-cqupt-lqy11/tmp/coupon-task/pending-users.xlsx', NULL, 120, '0,2', 910000000000002, 1, '2026-05-01 09:00:00', 0, NULL, '2026-04-24 09:00:00', @merchant_user, '2026-04-24 09:00:00', 0),
(940000000000002, @merchant_shop, 202604270002, '执行中-新品券推送', 'E:/IdeaProjects/mall-cqupt-lqy11/tmp/coupon-task/running-users.xlsx', NULL, 80, '1', 910000000000001, 0, '2026-04-27 09:00:00', 1, NULL, '2026-04-24 09:10:00', @merchant_user, '2026-04-27 09:01:00', 0),
(940000000000003, @merchant_shop, 202604270003, '失败-手机号异常名单', 'E:/IdeaProjects/mall-cqupt-lqy11/tmp/coupon-task/failed-users.xlsx', 'E:/IdeaProjects/mall-cqupt-lqy11/tmp/coupon-task/failed-result.xlsx', 50, '3', 910000000000003, 0, '2026-04-25 10:00:00', 2, '2026-04-25 10:05:00', '2026-04-24 09:20:00', @merchant_user, '2026-04-25 10:05:00', 0),
(940000000000004, @merchant_shop, 202604270004, '成功-商品专属券推送', 'E:/IdeaProjects/mall-cqupt-lqy11/tmp/coupon-task/success-users.xlsx', NULL, 200, '0,1,2', 910000000000004, 0, '2026-04-25 11:00:00', 3, '2026-04-25 11:08:00', '2026-04-24 09:30:00', @merchant_user, '2026-04-25 11:08:00', 0),
(940000000000005, @merchant_shop, 202604270005, '取消-过期活动券推送', 'E:/IdeaProjects/mall-cqupt-lqy11/tmp/coupon-task/canceled-users.xlsx', NULL, 30, '2', 910000000000007, 1, '2026-04-26 09:00:00', 4, NULL, '2026-04-24 09:40:00', @merchant_user, '2026-04-24 18:00:00', 0)
ON DUPLICATE KEY UPDATE
    batch_id = VALUES(batch_id),
    task_name = VALUES(task_name),
    file_address = VALUES(file_address),
    fail_file_address = VALUES(fail_file_address),
    send_num = VALUES(send_num),
    notify_type = VALUES(notify_type),
    coupon_template_id = VALUES(coupon_template_id),
    send_type = VALUES(send_type),
    send_time = VALUES(send_time),
    status = VALUES(status),
    completion_time = VALUES(completion_time),
    operator_id = VALUES(operator_id),
    update_time = VALUES(update_time),
    del_flag = VALUES(del_flag);

SET FOREIGN_KEY_CHECKS = 1;

-- Next step: run resources/database/frontend-test-redis-warmup.redis with redis-cli
-- so cart, wallet, coupon center and settlement pages can read cached templates/goods.
