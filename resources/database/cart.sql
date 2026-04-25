-- ============================================================
-- 购物车模块数据库脚本
-- 数据库：mall_coupon_cqupt_0 / mall_coupon_cqupt_1
-- 说明：购物车表，按 user_id 分库分表
-- ============================================================

CREATE TABLE IF NOT EXISTS `t_cart` (
    `id`          bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '购物车项ID',
    `user_id`     bigint(20)    NOT NULL                COMMENT '用户ID（分片键）',
    `goods_id`    bigint(20)    NOT NULL                COMMENT '商品ID',
    `shop_number` bigint(20)    NOT NULL                COMMENT '店铺编号',
    `quantity`    int(11)       NOT NULL DEFAULT 1      COMMENT '商品数量',
    `selected`    tinyint(1)    NOT NULL DEFAULT 1      COMMENT '是否选中 0:未选中 1:选中',
    `create_time` datetime      DEFAULT NULL            COMMENT '创建时间',
    `update_time` datetime      DEFAULT NULL            COMMENT '修改时间',
    `del_flag`    tinyint(1)    DEFAULT 0               COMMENT '删除标识 0:未删除 1:已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_goods` (`user_id`, `goods_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_shop_number` (`shop_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';
