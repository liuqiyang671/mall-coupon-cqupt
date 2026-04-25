-- ============================================================
-- 商品管理模块数据库脚本
-- 数据库：mall_coupon_cqupt_0 / mall_coupon_cqupt_1
-- 说明：商品管理相关表，按 shop_number 分库分表
-- ============================================================

-- ============================================================
-- 1. 商品分类表（广播表，每个库一份）
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_goods_category` (
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `parent_id`   bigint(20)   NOT NULL DEFAULT 0      COMMENT '父分类ID，0表示一级分类',
    `name`        varchar(64)  NOT NULL                 COMMENT '分类名称',
    `icon`        varchar(512) DEFAULT NULL             COMMENT '分类图标URL',
    `sort_order`  int(11)      NOT NULL DEFAULT 0      COMMENT '排序值，越小越靠前',
    `level`       tinyint(1)   NOT NULL DEFAULT 1      COMMENT '分类层级 1:一级 2:二级 3:三级',
    `status`      tinyint(1)   NOT NULL DEFAULT 0      COMMENT '状态 0:启用 1:禁用',
    `create_time` datetime     DEFAULT NULL             COMMENT '创建时间',
    `update_time` datetime     DEFAULT NULL             COMMENT '修改时间',
    `del_flag`    tinyint(1)   DEFAULT 0                COMMENT '删除标识 0:未删除 1:已删除',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- ============================================================
-- 2. 商品基本信息表（分片表，按 shop_number 分库分表）
--    物理表：t_goods_${0..15} 在 ds_0 和 ds_1
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_goods` (
    `id`             bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `shop_number`    bigint(20)    NOT NULL                COMMENT '店铺编号（分片键）',
    `category_id`    bigint(20)    NOT NULL                COMMENT '分类ID',
    `name`           varchar(256)  NOT NULL                COMMENT '商品名称',
    `description`    text          DEFAULT NULL            COMMENT '商品描述',
    `main_image`     varchar(512)  DEFAULT NULL            COMMENT '主图URL',
    `price`          decimal(10,2) NOT NULL                COMMENT '商品价格',
    `original_price` decimal(10,2) DEFAULT NULL            COMMENT '原价',
    `stock`          int(11)       NOT NULL DEFAULT 0      COMMENT '库存数量',
    `sales`          int(11)       NOT NULL DEFAULT 0      COMMENT '销量',
    `unit`           varchar(16)   DEFAULT '件'            COMMENT '计量单位',
    `status`         tinyint(1)    NOT NULL DEFAULT 0      COMMENT '状态 0:下架 1:上架 2:违规下架',
    `sort_order`     int(11)       NOT NULL DEFAULT 0      COMMENT '排序值',
    `create_time`    datetime      DEFAULT NULL            COMMENT '创建时间',
    `update_time`    datetime      DEFAULT NULL            COMMENT '修改时间',
    `del_flag`       tinyint(1)    DEFAULT 0               COMMENT '删除标识 0:未删除 1:已删除',
    PRIMARY KEY (`id`),
    KEY `idx_shop_number` (`shop_number`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`),
    KEY `idx_status_stock` (`status`, `stock`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品基本信息表';

-- ============================================================
-- 3. 商品图片表（分片表，按 shop_number 分库分表）
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_goods_image` (
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '图片ID',
    `shop_number` bigint(20)   NOT NULL                COMMENT '店铺编号（分片键）',
    `goods_id`    bigint(20)   NOT NULL                COMMENT '商品ID',
    `image_url`   varchar(512) NOT NULL                COMMENT '图片URL',
    `sort_order`  int(11)      NOT NULL DEFAULT 0      COMMENT '排序值',
    `create_time` datetime     DEFAULT NULL            COMMENT '创建时间',
    `del_flag`    tinyint(1)   DEFAULT 0               COMMENT '删除标识 0:未删除 1:已删除',
    PRIMARY KEY (`id`),
    KEY `idx_shop_number` (`shop_number`),
    KEY `idx_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品图片表';

-- ============================================================
-- 4. 商品属性定义表（广播表）
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_goods_attribute` (
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '属性ID',
    `name`        varchar(64)  NOT NULL                COMMENT '属性名称',
    `input_type`  tinyint(1)   NOT NULL DEFAULT 0      COMMENT '输入类型 0:文本输入 1:单选 2:多选',
    `values`      varchar(1024) DEFAULT NULL            COMMENT '可选值列表，逗号分隔',
    `sort_order`  int(11)      NOT NULL DEFAULT 0      COMMENT '排序值',
    `status`      tinyint(1)   NOT NULL DEFAULT 0      COMMENT '状态 0:启用 1:禁用',
    `create_time` datetime     DEFAULT NULL            COMMENT '创建时间',
    `update_time` datetime     DEFAULT NULL            COMMENT '修改时间',
    `del_flag`    tinyint(1)   DEFAULT 0               COMMENT '删除标识 0:未删除 1:已删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品属性定义表';

-- ============================================================
-- 5. 商品属性值表（分片表，按 shop_number 分库分表）
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_goods_attribute_value` (
    `id`            bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '属性值ID',
    `shop_number`   bigint(20)  NOT NULL                COMMENT '店铺编号（分片键）',
    `goods_id`      bigint(20)  NOT NULL                COMMENT '商品ID',
    `attribute_id`  bigint(20)  NOT NULL                COMMENT '属性ID',
    `attribute_value` varchar(256) NOT NULL              COMMENT '属性值',
    `create_time`   datetime    DEFAULT NULL            COMMENT '创建时间',
    `del_flag`      tinyint(1)  DEFAULT 0               COMMENT '删除标识 0:未删除 1:已删除',
    PRIMARY KEY (`id`),
    KEY `idx_shop_number` (`shop_number`),
    KEY `idx_goods_id` (`goods_id`),
    KEY `idx_attribute_id` (`attribute_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品属性值表';
