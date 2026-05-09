-- ============================================================
-- 电商平台商品与购物车分库分表完整方案
-- ============================================================
-- 项目名称: mall-cqupt-lqy11
-- 创建时间: 2026-04-27
-- 数据库: MySQL 8.0+
-- 中间件: ShardingSphere-JDBC 5.x
-- ============================================================
--
-- 一、数据量评估与分库分表必要性分析
-- ============================================================
-- 
-- 1. 当前业务特征分析:
--    - 商品数据: 预计单店铺1000-5000个SKU, 10000个店铺 = 5000万商品记录
--    - 购物车数据: 活跃用户人均20个购物车项, 100万活跃用户 = 2000万购物车记录
--    - 日活用户: 预计10万DAU, 购物车读写比约 10:1
--    - 商品访问: 日均PV 500万+, 高峰时段QPS 5000+
--
-- 2. 分库分表阈值评估:
--    +----------------------+----------+----------+---------+
--    | 指标                 | 阈值     | 当前预估 | 是否超标|
--    +----------------------+----------+----------+---------+
--    | 商品表记录数         | 1000万   | 5000万   | 是      |
--    | 购物车记录数         | 5000万   | 2000万   | 接近    |
--    | 单表查询响应时间     | <100ms   | 150-300ms| 是      |
--    | 单表写入QPS          | <3000    | 5000+    | 是      |
--    +----------------------+----------+----------+---------+
--
-- 3. 结论: 必须实施分库分表策略
--
-- ============================================================
-- 二、分库分表方案设计
-- ============================================================
--
-- 1. 分库策略:
--    - 按业务模块分库 + 按业务键哈希分库
--    - 商品模块: mall_goods_ds_${0..3} (4个库)
--    - 购物车模块: mall_cart_ds_${0..3} (4个库)
--    - 说明: 商品和购物车读写特征不同,分库隔离避免相互影响
--
-- 2. 分表策略:
--    - 商品表(t_goods): 按 shop_number HASH 分表, 每个库8张表, 共32张
--    - 商品图片表(t_goods_image): 按 shop_number HASH 分表, 每个库8张表
--    - 商品属性表(t_goods_attribute_value): 按 shop_number HASH 分表
--    - 购物车表(t_cart): 按 user_id HASH 分表, 每个库8张表, 共32张
--
-- 3. 分片数量规划:
--    +----------------+-----------+----------+--------+
--    | 表名           | 分库数    | 每库表数 | 总表数 |
--    +----------------+-----------+----------+--------+
--    | t_goods        | 4         | 8        | 32     |
--    | t_goods_image  | 4         | 8        | 32     |
--    | t_goods_attr   | 4         | 8        | 32     |
--    | t_cart         | 4         | 8        | 32     |
--    +----------------+-----------+----------+--------+
--
-- 4. 路由规则:
--    - 商品路由: database_index = shop_number % 4
--               table_index = (shop_number / 4) % 8
--    - 购物车路由: database_index = user_id % 4
--                 table_index = (user_id / 4) % 8
--
-- 5. 命名规则:
--    - 数据库: mall_goods_ds_0, mall_goods_ds_1, mall_goods_ds_2, mall_goods_ds_3
--             mall_cart_ds_0, mall_cart_ds_1, mall_cart_ds_2, mall_cart_ds_3
--    - 数据表: t_goods_0, t_goods_1, ..., t_goods_7
--             t_cart_0, t_cart_1, ..., t_cart_7
--
-- 6. 中间件选择:
--    - 已采用: ShardingSphere-JDBC 5.x
--    - 优势: 轻量级,与应用集成,支持自定义分片算法
--    - 现有自定义算法: DBHashModShardingAlgorithm, TableHashModShardingAlgorithm
--
-- ============================================================
-- 三、完整SQL脚本
-- ============================================================

-- ============================================================
-- 3.1 商品模块 - 创建数据库
-- ============================================================

CREATE DATABASE IF NOT EXISTS `mall_goods_ds_0` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS `mall_goods_ds_1` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS `mall_goods_ds_2` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS `mall_goods_ds_3` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- ============================================================
-- 3.2 购物车模块 - 创建数据库
-- ============================================================

CREATE DATABASE IF NOT EXISTS `mall_cart_ds_0` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS `mall_cart_ds_1` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS `mall_cart_ds_2` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS `mall_cart_ds_3` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- ============================================================
-- 3.3 商品模块 - 表结构定义 (每个库执行)
-- ============================================================

-- 以下脚本需在 mall_goods_ds_0, mall_goods_ds_1, mall_goods_ds_2, mall_goods_ds_3 中各执行一次

USE mall_goods_ds_0;
-- USE mall_goods_ds_1;
-- USE mall_goods_ds_2;
-- USE mall_goods_ds_3;

-- ============================================================
-- 3.3.1 商品分类表 (广播表, 每个库独立一份, 不分片)
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_goods_category` (
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `parent_id`   bigint(20)   NOT NULL DEFAULT 0      COMMENT '父分类ID, 0表示一级分类',
    `name`        varchar(64)  NOT NULL                 COMMENT '分类名称',
    `icon`        varchar(512) DEFAULT NULL             COMMENT '分类图标URL',
    `sort_order`  int(11)      NOT NULL DEFAULT 0      COMMENT '排序值, 越小越靠前',
    `level`       tinyint(1)   NOT NULL DEFAULT 1      COMMENT '分类层级 1:一级 2:二级 3:三级',
    `status`      tinyint(1)   NOT NULL DEFAULT 0      COMMENT '状态 0:启用 1:禁用',
    `create_time` datetime     DEFAULT NULL             COMMENT '创建时间',
    `update_time` datetime     DEFAULT NULL             COMMENT '修改时间',
    `del_flag`    tinyint(1)   DEFAULT 0                COMMENT '删除标识 0:未删除 1:已删除',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_sort_order` (`sort_order`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- ============================================================
-- 3.3.2 商品基本信息表 (分片表, 按 shop_number 分库分表)
--       物理表: t_goods_0 ~ t_goods_7
-- ============================================================

CREATE TABLE IF NOT EXISTS `t_goods_0` LIKE `t_goods_template`;
CREATE TABLE IF NOT EXISTS `t_goods_1` LIKE `t_goods_template`;
CREATE TABLE IF NOT EXISTS `t_goods_2` LIKE `t_goods_template`;
CREATE TABLE IF NOT EXISTS `t_goods_3` LIKE `t_goods_template`;
CREATE TABLE IF NOT EXISTS `t_goods_4` LIKE `t_goods_template`;
CREATE TABLE IF NOT EXISTS `t_goods_5` LIKE `t_goods_template`;
CREATE TABLE IF NOT EXISTS `t_goods_6` LIKE `t_goods_template`;
CREATE TABLE IF NOT EXISTS `t_goods_7` LIKE `t_goods_template`;

-- 创建模板表用于复制结构
CREATE TABLE IF NOT EXISTS `t_goods_template` (
    `id`             bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `shop_number`    bigint(20)    NOT NULL                COMMENT '店铺编号(分片键)',
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
    PRIMARY KEY (`id`, `shop_number`),
    UNIQUE KEY `uk_shop_goods` (`shop_number`, `id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`),
    KEY `idx_status_stock` (`status`, `stock`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品基本信息表';

-- ============================================================
-- 3.3.3 商品图片表 (分片表, 按 shop_number 分库分表)
--       物理表: t_goods_image_0 ~ t_goods_image_7
-- ============================================================

CREATE TABLE IF NOT EXISTS `t_goods_image_template` (
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '图片ID',
    `shop_number` bigint(20)   NOT NULL                COMMENT '店铺编号(分片键)',
    `goods_id`    bigint(20)   NOT NULL                COMMENT '商品ID',
    `image_url`   varchar(512) NOT NULL                COMMENT '图片URL',
    `image_type`  tinyint(1)   NOT NULL DEFAULT 0      COMMENT '图片类型 0:主图 1:详情图 2:规格图',
    `sort_order`  int(11)      NOT NULL DEFAULT 0      COMMENT '排序值',
    `create_time` datetime     DEFAULT NULL            COMMENT '创建时间',
    `del_flag`    tinyint(1)   DEFAULT 0               COMMENT '删除标识 0:未删除 1:已删除',
    PRIMARY KEY (`id`, `shop_number`),
    KEY `idx_goods_id` (`goods_id`),
    KEY `idx_image_type` (`image_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品图片表';

CREATE TABLE IF NOT EXISTS `t_goods_image_0` LIKE `t_goods_image_template`;
CREATE TABLE IF NOT EXISTS `t_goods_image_1` LIKE `t_goods_image_template`;
CREATE TABLE IF NOT EXISTS `t_goods_image_2` LIKE `t_goods_image_template`;
CREATE TABLE IF NOT EXISTS `t_goods_image_3` LIKE `t_goods_image_template`;
CREATE TABLE IF NOT EXISTS `t_goods_image_4` LIKE `t_goods_image_template`;
CREATE TABLE IF NOT EXISTS `t_goods_image_5` LIKE `t_goods_image_template`;
CREATE TABLE IF NOT EXISTS `t_goods_image_6` LIKE `t_goods_image_template`;
CREATE TABLE IF NOT EXISTS `t_goods_image_7` LIKE `t_goods_image_template`;

-- ============================================================
-- 3.3.4 商品属性定义表 (广播表, 每个库独立一份)
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_goods_attribute` (
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '属性ID',
    `name`        varchar(64)  NOT NULL                COMMENT '属性名称',
    `input_type`  tinyint(1)   NOT NULL DEFAULT 0      COMMENT '输入类型 0:文本输入 1:单选 2:多选',
    `values`      varchar(1024) DEFAULT NULL            COMMENT '可选值列表, 逗号分隔',
    `sort_order`  int(11)      NOT NULL DEFAULT 0      COMMENT '排序值',
    `status`      tinyint(1)   NOT NULL DEFAULT 0      COMMENT '状态 0:启用 1:禁用',
    `create_time` datetime     DEFAULT NULL            COMMENT '创建时间',
    `update_time` datetime     DEFAULT NULL            COMMENT '修改时间',
    `del_flag`    tinyint(1)   DEFAULT 0               COMMENT '删除标识 0:未删除 1:已删除',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品属性定义表';

-- ============================================================
-- 3.3.5 商品属性值表 (分片表, 按 shop_number 分库分表)
--       物理表: t_goods_attribute_value_0 ~ t_goods_attribute_value_7
-- ============================================================

CREATE TABLE IF NOT EXISTS `t_goods_attribute_value_template` (
    `id`            bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '属性值ID',
    `shop_number`   bigint(20)  NOT NULL                COMMENT '店铺编号(分片键)',
    `goods_id`      bigint(20)  NOT NULL                COMMENT '商品ID',
    `attribute_id`  bigint(20)  NOT NULL                COMMENT '属性ID',
    `attribute_value` varchar(256) NOT NULL              COMMENT '属性值',
    `create_time`   datetime    DEFAULT NULL            COMMENT '创建时间',
    `del_flag`      tinyint(1)  DEFAULT 0               COMMENT '删除标识 0:未删除 1:已删除',
    PRIMARY KEY (`id`, `shop_number`),
    KEY `idx_goods_id` (`goods_id`),
    KEY `idx_attribute_id` (`attribute_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品属性值表';

CREATE TABLE IF NOT EXISTS `t_goods_attribute_value_0` LIKE `t_goods_attribute_value_template`;
CREATE TABLE IF NOT EXISTS `t_goods_attribute_value_1` LIKE `t_goods_attribute_value_template`;
CREATE TABLE IF NOT EXISTS `t_goods_attribute_value_2` LIKE `t_goods_attribute_value_template`;
CREATE TABLE IF NOT EXISTS `t_goods_attribute_value_3` LIKE `t_goods_attribute_value_template`;
CREATE TABLE IF NOT EXISTS `t_goods_attribute_value_4` LIKE `t_goods_attribute_value_template`;
CREATE TABLE IF NOT EXISTS `t_goods_attribute_value_5` LIKE `t_goods_attribute_value_template`;
CREATE TABLE IF NOT EXISTS `t_goods_attribute_value_6` LIKE `t_goods_attribute_value_template`;
CREATE TABLE IF NOT EXISTS `t_goods_attribute_value_7` LIKE `t_goods_attribute_value_template`;

-- ============================================================
-- 3.3.6 商品库存表 (分片表, 按 shop_number 分库分表)
--       物理表: t_goods_stock_0 ~ t_goods_stock_7
--       说明: 将库存独立成表, 提高库存更新的并发性能
-- ============================================================

CREATE TABLE IF NOT EXISTS `t_goods_stock_template` (
    `id`             bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `shop_number`    bigint(20) NOT NULL                COMMENT '店铺编号(分片键)',
    `goods_id`       bigint(20) NOT NULL                COMMENT '商品ID',
    `sku_id`         bigint(20) DEFAULT NULL            COMMENT 'SKU ID',
    `stock`          int(11)    NOT NULL DEFAULT 0      COMMENT '可用库存',
    `lock_stock`     int(11)    NOT NULL DEFAULT 0      COMMENT '锁定库存',
    `total_stock`    int(11)    NOT NULL DEFAULT 0      COMMENT '总库存',
    `low_stock_threshold` int(11) NOT NULL DEFAULT 10   COMMENT '低库存预警阈值',
    `version`        int(11)    NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    `create_time`    datetime   DEFAULT NULL            COMMENT '创建时间',
    `update_time`    datetime   DEFAULT NULL            COMMENT '修改时间',
    `del_flag`       tinyint(1) DEFAULT 0               COMMENT '删除标识 0:未删除 1:已删除',
    PRIMARY KEY (`id`, `shop_number`),
    UNIQUE KEY `uk_goods_sku` (`shop_number`, `goods_id`, `sku_id`),
    KEY `idx_stock` (`stock`),
    KEY `idx_low_stock` (`stock`, `low_stock_threshold`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品库存表';

CREATE TABLE IF NOT EXISTS `t_goods_stock_0` LIKE `t_goods_stock_template`;
CREATE TABLE IF NOT EXISTS `t_goods_stock_1` LIKE `t_goods_stock_template`;
CREATE TABLE IF NOT EXISTS `t_goods_stock_2` LIKE `t_goods_stock_template`;
CREATE TABLE IF NOT EXISTS `t_goods_stock_3` LIKE `t_goods_stock_template`;
CREATE TABLE IF NOT EXISTS `t_goods_stock_4` LIKE `t_goods_stock_template`;
CREATE TABLE IF NOT EXISTS `t_goods_stock_5` LIKE `t_goods_stock_template`;
CREATE TABLE IF NOT EXISTS `t_goods_stock_6` LIKE `t_goods_stock_template`;
CREATE TABLE IF NOT EXISTS `t_goods_stock_7` LIKE `t_goods_stock_template`;

-- ============================================================
-- 3.4 购物车模块 - 表结构定义 (每个库执行)
-- ============================================================

-- 以下脚本需在 mall_cart_ds_0, mall_cart_ds_1, mall_cart_ds_2, mall_cart_ds_3 中各执行一次

USE mall_cart_ds_0;
-- USE mall_cart_ds_1;
-- USE mall_cart_ds_2;
-- USE mall_cart_ds_3;

-- ============================================================
-- 3.4.1 购物车表 (分片表, 按 user_id 分库分表)
--       物理表: t_cart_0 ~ t_cart_7
-- ============================================================

CREATE TABLE IF NOT EXISTS `t_cart_template` (
    `id`          bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '购物车项ID',
    `user_id`     bigint(20)    NOT NULL                COMMENT '用户ID(分片键)',
    `goods_id`    bigint(20)    NOT NULL                COMMENT '商品ID',
    `shop_number` bigint(20)    NOT NULL                COMMENT '店铺编号',
    `sku_id`      bigint(20)    DEFAULT NULL            COMMENT 'SKU ID',
    `quantity`    int(11)       NOT NULL DEFAULT 1      COMMENT '商品数量',
    `selected`    tinyint(1)    NOT NULL DEFAULT 1      COMMENT '是否选中 0:未选中 1:选中',
    `add_time`    datetime      DEFAULT NULL            COMMENT '加入购物车时间',
    `update_time` datetime      DEFAULT NULL            COMMENT '修改时间',
    `expire_time` datetime      DEFAULT NULL            COMMENT '过期时间(可选, 用于清理无效购物车数据)',
    `del_flag`    tinyint(1)    DEFAULT 0               COMMENT '删除标识 0:未删除 1:已删除',
    PRIMARY KEY (`id`, `user_id`),
    UNIQUE KEY `uk_user_goods_sku` (`user_id`, `goods_id`, `sku_id`),
    KEY `idx_shop_number` (`shop_number`),
    KEY `idx_add_time` (`add_time`),
    KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

CREATE TABLE IF NOT EXISTS `t_cart_0` LIKE `t_cart_template`;
CREATE TABLE IF NOT EXISTS `t_cart_1` LIKE `t_cart_template`;
CREATE TABLE IF NOT EXISTS `t_cart_2` LIKE `t_cart_template`;
CREATE TABLE IF NOT EXISTS `t_cart_3` LIKE `t_cart_template`;
CREATE TABLE IF NOT EXISTS `t_cart_4` LIKE `t_cart_template`;
CREATE TABLE IF NOT EXISTS `t_cart_5` LIKE `t_cart_template`;
CREATE TABLE IF NOT EXISTS `t_cart_6` LIKE `t_cart_template`;
CREATE TABLE IF NOT EXISTS `t_cart_7` LIKE `t_cart_template`;

-- ============================================================
-- 3.4.2 购物车历史表 (按月份分表, 用于归档过期数据)
--       物理表: t_cart_history_202604, t_cart_history_202605, ...
-- ============================================================

CREATE TABLE IF NOT EXISTS `t_cart_history_template` (
    `id`          bigint(20)    NOT NULL COMMENT '购物车项ID',
    `user_id`     bigint(20)    NOT NULL                COMMENT '用户ID',
    `goods_id`    bigint(20)    NOT NULL                COMMENT '商品ID',
    `shop_number` bigint(20)    NOT NULL                COMMENT '店铺编号',
    `sku_id`      bigint(20)    DEFAULT NULL            COMMENT 'SKU ID',
    `quantity`    int(11)       NOT NULL DEFAULT 1      COMMENT '商品数量',
    `archive_time` datetime     DEFAULT NULL            COMMENT '归档时间',
    `original_add_time` datetime DEFAULT NULL           COMMENT '原始加入购物车时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_archive_time` (`archive_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车历史归档表';

-- 示例: 创建历史归档表(按月)
CREATE TABLE IF NOT EXISTS `t_cart_history_202604` LIKE `t_cart_history_template`;
CREATE TABLE IF NOT EXISTS `t_cart_history_202605` LIKE `t_cart_history_template`;

-- ============================================================
-- 四、分片路由配置 (ShardingSphere YAML配置)
-- ============================================================
-- 以下为 merchant-admin 模块的 shardingsphere-config.yaml 配置示例
-- ============================================================

/*
dataSources:
  goods_ds_0:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://127.0.0.1:3306/mall_goods_ds_0?useUnicode=true&characterEncoding=UTF-8&rewriteBatchedStatements=true&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD}
  goods_ds_1:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://127.0.0.1:3306/mall_goods_ds_1?useUnicode=true&characterEncoding=UTF-8&rewriteBatchedStatements=true&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD}
  goods_ds_2:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://127.0.0.1:3306/mall_goods_ds_2?useUnicode=true&characterEncoding=UTF-8&rewriteBatchedStatements=true&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD}
  goods_ds_3:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://127.0.0.1:3306/mall_goods_ds_3?useUnicode=true&characterEncoding=UTF-8&rewriteBatchedStatements=true&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD}
  cart_ds_0:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://127.0.0.1:3306/mall_cart_ds_0?useUnicode=true&characterEncoding=UTF-8&rewriteBatchedStatements=true&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD}
  cart_ds_1:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://127.0.0.1:3306/mall_cart_ds_1?useUnicode=true&characterEncoding=UTF-8&rewriteBatchedStatements=true&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD}
  cart_ds_2:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://127.0.0.1:3306/mall_cart_ds_2?useUnicode=true&characterEncoding=UTF-8&rewriteBatchedStatements=true&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD}
  cart_ds_3:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://127.0.0.1:3306/mall_cart_ds_3?useUnicode=true&characterEncoding=UTF-8&rewriteBatchedStatements=true&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD}

rules:
  - !SHARDING
    tables:
      t_goods:
        actualDataNodes: goods_ds_${0..3}.t_goods_${0..7}
        databaseStrategy:
          standard:
            shardingColumn: shop_number
            shardingAlgorithmName: goods_database_mod
        tableStrategy:
          standard:
            shardingColumn: shop_number
            shardingAlgorithmName: goods_table_mod
      t_goods_image:
        actualDataNodes: goods_ds_${0..3}.t_goods_image_${0..7}
        databaseStrategy:
          standard:
            shardingColumn: shop_number
            shardingAlgorithmName: goods_image_database_mod
        tableStrategy:
          standard:
            shardingColumn: shop_number
            shardingAlgorithmName: goods_image_table_mod
      t_goods_attribute_value:
        actualDataNodes: goods_ds_${0..3}.t_goods_attribute_value_${0..7}
        databaseStrategy:
          standard:
            shardingColumn: shop_number
            shardingAlgorithmName: goods_attr_database_mod
        tableStrategy:
          standard:
            shardingColumn: shop_number
            shardingAlgorithmName: goods_attr_table_mod
      t_goods_stock:
        actualDataNodes: goods_ds_${0..3}.t_goods_stock_${0..7}
        databaseStrategy:
          standard:
            shardingColumn: shop_number
            shardingAlgorithmName: goods_stock_database_mod
        tableStrategy:
          standard:
            shardingColumn: shop_number
            shardingAlgorithmName: goods_stock_table_mod
      t_cart:
        actualDataNodes: cart_ds_${0..3}.t_cart_${0..7}
        databaseStrategy:
          standard:
            shardingColumn: user_id
            shardingAlgorithmName: cart_database_mod
        tableStrategy:
          standard:
            shardingColumn: user_id
            shardingAlgorithmName: cart_table_mod

    broadcastTables:
      - t_goods_category
      - t_goods_attribute

    shardingAlgorithms:
      goods_database_mod:
        type: INLINE
        props:
          algorithm-expression: goods_ds_${shop_number % 4}
      goods_table_mod:
        type: INLINE
        props:
          algorithm-expression: t_goods_${(shop_number / 4) % 8}
      goods_image_database_mod:
        type: INLINE
        props:
          algorithm-expression: goods_ds_${shop_number % 4}
      goods_image_table_mod:
        type: INLINE
        props:
          algorithm-expression: t_goods_image_${(shop_number / 4) % 8}
      goods_attr_database_mod:
        type: INLINE
        props:
          algorithm-expression: goods_ds_${shop_number % 4}
      goods_attr_table_mod:
        type: INLINE
        props:
          algorithm-expression: t_goods_attribute_value_${(shop_number / 4) % 8}
      goods_stock_database_mod:
        type: INLINE
        props:
          algorithm-expression: goods_ds_${shop_number % 4}
      goods_stock_table_mod:
        type: INLINE
        props:
          algorithm-expression: t_goods_stock_${(shop_number / 4) % 8}
      cart_database_mod:
        type: INLINE
        props:
          algorithm-expression: cart_ds_${user_id % 4}
      cart_table_mod:
        type: INLINE
        props:
          algorithm-expression: t_cart_${(user_id / 4) % 8}

props:
  sql-show: true
*/

-- ============================================================
-- 五、分表间关联关系处理方案
-- ============================================================

-- 5.1 商品与购物车的跨库关联
--     问题: 商品按shop_number分片, 购物车按user_id分片, 无法直接JOIN
--     解决方案:
--     方案A: 应用层组装 - 先查购物车获取goods_id列表, 再根据goods_id反查shop_number定位商品分片
--     方案B: 冗余数据 - 购物车中冗余商品快照信息(名称, 价格, 主图), 减少实时关联查询
--     方案C: 全局表 - 将高频查询的商品基础信息同步到Redis或全局索引表

-- 5.2 商品关联表的一致性
--     t_goods, t_goods_image, t_goods_attribute_value, t_goods_stock 均按 shop_number 分片
--     同一店铺的所有商品相关数据在同一分片, 保证店铺维度事务一致性

-- 5.3 分布式事务处理
--     使用场景: 创建商品时需同时写入 t_goods 和 t_goods_stock
--     解决方案:
--     - 同分片: 使用本地事务 @Transactional
--     - 跨分片: 使用 Seata AT 模式或基于消息队列的最终一致性方案

-- ============================================================
-- 六、索引设计说明
-- ============================================================

-- 6.1 商品表索引策略:
--     +------------------------+------------------+--------------------+-------------------+
--     | 索引名称               | 索引字段         | 索引类型           | 使用场景          |
--     +------------------------+------------------+--------------------+-------------------+
--     | PRIMARY                | id, shop_number  | 聚簇索引           | 主键查询          |
--     | uk_shop_goods          | shop_number, id  | 唯一索引           | 防止重复创建      |
--     | idx_category_id        | category_id      | 普通索引           | 分类查询          |
--     | idx_status             | status           | 普通索引           | 状态筛选          |
--     | idx_status_stock       | status, stock    | 联合索引           | 可售商品查询      |
--     | idx_create_time        | create_time      | 普通索引           | 时间范围查询      |
--     +------------------------+------------------+--------------------+-------------------+

-- 6.2 购物车表索引策略:
--     +------------------------+---------------------------+--------+-------------------+
--     | 索引名称               | 索引字段                  | 类型   | 使用场景          |
--     +------------------------+---------------------------+--------+-------------------+
--     | PRIMARY                | id, user_id               | 聚簇   | 主键查询          |
--     | uk_user_goods_sku      | user_id, goods_id, sku_id | 唯一   | 防止重复添加      |
--     | idx_shop_number        | shop_number               | 普通   | 店铺维度统计      |
--     | idx_add_time           | add_time                  | 普通   | 时间排序          |
--     | idx_expire_time        | expire_time               | 普通   | 过期数据清理      |
--     +------------------------+---------------------------+--------+-------------------+

-- ============================================================
-- 七、数据迁移脚本
-- ============================================================

-- 7.1 迁移前置准备
--     a. 停止相关写入服务
--     b. 备份现有数据库:
--        mysqldump -u root -p mall_coupon_cqupt_0 t_goods t_goods_image t_goods_attribute_value t_cart > backup_$(date +%Y%m%d_%H%M%S).sql

-- 7.2 数据迁移脚本 (从旧表迁移到新分片表)

-- 迁移商品数据
-- 说明: 此脚本需要在应用层或使用存储过程实现, 根据 shop_number 计算目标分片

DELIMITER //

CREATE PROCEDURE IF NOT EXISTS migrate_goods_data()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_id BIGINT;
    DECLARE v_shop_number BIGINT;
    DECLARE v_category_id BIGINT;
    DECLARE v_name VARCHAR(256);
    DECLARE v_description TEXT;
    DECLARE v_main_image VARCHAR(512);
    DECLARE v_price DECIMAL(10,2);
    DECLARE v_original_price DECIMAL(10,2);
    DECLARE v_stock INT;
    DECLARE v_sales INT;
    DECLARE v_unit VARCHAR(16);
    DECLARE v_status TINYINT;
    DECLARE v_sort_order INT;
    DECLARE v_create_time DATETIME;
    DECLARE v_update_time DATETIME;
    DECLARE v_del_flag TINYINT;
    DECLARE v_db_index INT;
    DECLARE v_table_index INT;
    DECLARE v_target_table VARCHAR(64);
    
    DECLARE cur CURSOR FOR 
        SELECT id, shop_number, category_id, name, description, main_image, price, 
               original_price, stock, sales, unit, status, sort_order, 
               create_time, update_time, del_flag
        FROM old_t_goods;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN cur;
    
    read_loop: LOOP
        FETCH cur INTO v_id, v_shop_number, v_category_id, v_name, v_description, v_main_image, v_price,
                       v_original_price, v_stock, v_sales, v_unit, v_status, v_sort_order,
                       v_create_time, v_update_time, v_del_flag;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        SET v_db_index = v_shop_number % 4;
        SET v_table_index = (v_shop_number DIV 4) % 8;
        SET v_target_table = CONCAT('mall_goods_ds_', v_db_index, '.t_goods_', v_table_index);
        
        SET @sql = CONCAT('INSERT INTO ', v_target_table, 
                         ' (id, shop_number, category_id, name, description, main_image, price, ',
                         'original_price, stock, sales, unit, status, sort_order, ',
                         'create_time, update_time, del_flag) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)');
        
        PREPARE stmt FROM @sql;
        SET @p_id = v_id;
        SET @p_shop = v_shop_number;
        SET @p_cat = v_category_id;
        SET @p_name = v_name;
        SET @p_desc = v_description;
        SET @p_img = v_main_image;
        SET @p_price = v_price;
        SET @p_orig_price = v_original_price;
        SET @p_stock = v_stock;
        SET @p_sales = v_sales;
        SET @p_unit = v_unit;
        SET @p_status = v_status;
        SET @p_sort = v_sort_order;
        SET @p_create = v_create_time;
        SET @p_update = v_update_time;
        SET @p_del = v_del_flag;
        
        EXECUTE stmt USING @p_id, @p_shop, @p_cat, @p_name, @p_desc, @p_img, @p_price,
                           @p_orig_price, @p_stock, @p_sales, @p_unit, @p_status, @p_sort,
                           @p_create, @p_update, @p_del;
        DEALLOCATE PREPARE stmt;
        
    END LOOP;
    
    CLOSE cur;
END //

DELIMITER ;

-- 迁移购物车数据
DELIMITER //

CREATE PROCEDURE IF NOT EXISTS migrate_cart_data()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_id BIGINT;
    DECLARE v_user_id BIGINT;
    DECLARE v_goods_id BIGINT;
    DECLARE v_shop_number BIGINT;
    DECLARE v_quantity INT;
    DECLARE v_selected TINYINT;
    DECLARE v_create_time DATETIME;
    DECLARE v_update_time DATETIME;
    DECLARE v_del_flag TINYINT;
    DECLARE v_db_index INT;
    DECLARE v_table_index INT;
    DECLARE v_target_table VARCHAR(64);
    
    DECLARE cur CURSOR FOR 
        SELECT id, user_id, goods_id, shop_number, quantity, selected, 
               create_time, update_time, del_flag
        FROM old_t_cart;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN cur;
    
    read_loop: LOOP
        FETCH cur INTO v_id, v_user_id, v_goods_id, v_shop_number, v_quantity, v_selected,
                       v_create_time, v_update_time, v_del_flag;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        SET v_db_index = v_user_id % 4;
        SET v_table_index = (v_user_id DIV 4) % 8;
        SET v_target_table = CONCAT('mall_cart_ds_', v_db_index, '.t_cart_', v_table_index);
        
        SET @sql = CONCAT('INSERT INTO ', v_target_table,
                         ' (id, user_id, goods_id, shop_number, quantity, selected, ',
                         'add_time, update_time, del_flag) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)');
        
        PREPARE stmt FROM @sql;
        SET @p_id = v_id;
        SET @p_user = v_user_id;
        SET @p_goods = v_goods_id;
        SET @p_shop = v_shop_number;
        SET @p_qty = v_quantity;
        SET @p_sel = v_selected;
        SET @p_add = v_create_time;
        SET @p_update = v_update_time;
        SET @p_del = v_del_flag;
        
        EXECUTE stmt USING @p_id, @p_user, @p_goods, @p_shop, @p_qty, @p_sel,
                           @p_add, @p_update, @p_del;
        DEALLOCATE PREPARE stmt;
        
    END LOOP;
    
    CLOSE cur;
END //

DELIMITER ;

-- 执行迁移(需根据实际情况调整)
-- CALL migrate_goods_data();
-- CALL migrate_cart_data();

-- ============================================================
-- 八、数据一致性保障措施
-- ============================================================

-- 8.1 分库分表后的数据校验脚本

-- 校验商品数据迁移完整性
SELECT 
    'old_t_goods' AS table_name,
    COUNT(*) AS total_records,
    COUNT(DISTINCT shop_number) AS distinct_shops
FROM old_t_goods
UNION ALL
SELECT 
    CONCAT('t_goods_', table_index) AS table_name,
    COUNT(*) AS total_records,
    COUNT(DISTINCT shop_number) AS distinct_shops
FROM (
    SELECT 0 AS table_index FROM mall_goods_ds_0.t_goods_0
    UNION ALL SELECT 1 FROM mall_goods_ds_0.t_goods_1
    -- ... 其他分片表
) subquery
GROUP BY table_index;

-- 8.2 分布式ID生成策略
--     方案: 使用雪花算法(Snowflake)生成全局唯一ID
--     配置: 
--     - workerId: 根据服务实例分配
--     - datacenterId: 根据部署环境分配
--     - 保证跨分片ID唯一性

-- 8.3 全局时钟同步
--     所有数据库服务器配置NTP时钟同步, 确保时间字段一致性

-- 8.4 数据校验定时任务
DELIMITER //

CREATE EVENT IF NOT EXISTS check_sharding_consistency
ON SCHEDULE EVERY 1 DAY
STARTS '2026-04-28 02:00:00'
DO
BEGIN
    DECLARE v_old_count BIGINT;
    DECLARE v_new_count BIGINT;
    
    SELECT COUNT(*) INTO v_old_count FROM old_t_goods;
    SELECT COUNT(*) INTO v_new_count FROM mall_goods_ds_0.t_goods_0;
    -- 累加所有分片表记录数进行比较
    
    IF v_old_count != v_new_count THEN
        INSERT INTO data_consistency_log (check_time, table_name, old_count, new_count, status)
        VALUES (NOW(), 't_goods', v_old_count, v_new_count, 'INCONSISTENT');
    ELSE
        INSERT INTO data_consistency_log (check_time, table_name, old_count, new_count, status)
        VALUES (NOW(), 't_goods', v_old_count, v_new_count, 'CONSISTENT');
    END IF;
END //

DELIMITER ;

-- ============================================================
-- 九、性能优化建议
-- ============================================================

-- 9.1 读写分离配置
--     每个数据源配置主从架构:
--     - 主库: 处理写入操作
--     - 从库: 处理读取操作, 可配置多个只读实例
--     ShardingSphere配置:
/*
dataSources:
  goods_ds_0_primary:
    ...
  goods_ds_0_replica_0:
    ...
    
rules:
  - !READWRITE_SPLITTING
    dataSources:
      goods_ds_0_rw:
        writeDataSourceName: goods_ds_0_primary
        readDataSourceNames:
          - goods_ds_0_replica_0
*/

-- 9.2 缓存策略
--     a. 商品分类表: 全量缓存到Redis, 定时刷新
--     b. 热门商品: Redis缓存, 设置合理TTL
--     c. 购物车: Redis暂存, 异步持久化到数据库
--     d. 库存: Redis预扣减, 确保高并发场景下的性能

-- 9.3 批量操作优化
--     a. 批量插入: 使用 MyBatis-Plus 的 saveBatch, 每批500条
--     b. 批量更新: 分片键相同的记录可在同一分片批量更新
--     c. 批量查询: 按分片键分组后并行查询各分片

-- 9.4 分页查询优化
--     问题: 跨分片分页查询性能差
--     解决方案:
--     a. 禁止大范围跳页查询
--     b. 使用游标分页(基于最后一条记录ID)
--     c. 对于管理后台的全局查询, 使用ES等搜索引擎

-- 9.5 SQL优化建议
--     a. 避免跨分片JOIN, 改为应用层组装
--     b. 查询条件必须包含分片键
--     c. 避免使用 OR 条件连接不同分片键
--     d. 使用 EXPLAIN 分析执行计划

-- ============================================================
-- 十、扩容方案
-- ============================================================

-- 10.1 水平扩容触发条件:
--      - 单库数据量超过5000万
--      - 单表QPS持续超过5000
--      - 存储空间使用率超过80%

-- 10.2 扩容步骤:
--      a. 新增数据源: mall_goods_ds_4, mall_goods_ds_5, mall_goods_ds_6, mall_goods_ds_7
--      b. 数据迁移: 使用 ShardingSphere Scaling 或自定义迁移工具
--      c. 流量切换: 灰度切换读写路由到新的数据源
--      d. 验证: 数据一致性校验, 性能压测
--      e. 下线旧分片(可选)

-- 10.3 平滑扩容方案 - 基于一致性Hash
--      使用一致性Hash环, 扩容时仅迁移部分数据, 减少影响范围

-- ============================================================
-- 十一、监控与告警
-- ============================================================

-- 11.1 监控指标:
--      a. 各分片数据量分布均衡度
--      b. 各分片QPS/TPS
--      c. 慢SQL统计
--      d. 分片间数据延迟
--      e. 连接池使用率

-- 11.2 告警规则:
--      a. 单分片数据量偏差超过30% -> 检查分片键分布
--      b. 单分片QPS超过阈值 -> 考虑扩容
--      c. 慢SQL数量突增 -> 检查索引和SQL
--      d. 数据不一致 -> 触发数据修复流程

-- ============================================================
-- 十二、回滚方案
-- ============================================================

-- 12.1 回滚触发条件:
--      a. 分库分表后出现严重性能问题
--      b. 数据不一致且无法快速修复
--      c. 核心功能异常影响业务

-- 12.2 回滚步骤:
--      a. 停止写入服务
--      b. 从备份恢复数据到原库
--      c. 切换ShardingSphere配置回单库模式
--      d. 重启服务
--      e. 数据补录(迁移期间的新增数据)

-- 12.3 回滚数据补录脚本
--      记录迁移时间点, 将之后的变更数据重新写入原库

-- ============================================================
-- 脚本执行顺序说明:
-- ============================================================
-- 1. 执行 3.1, 3.2 创建数据库
-- 2. 在每个商品库执行 3.3 创建表结构
-- 3. 在每个购物车库执行 3.4 创建表结构
-- 4. 配置 ShardingSphere YAML (见第四部分注释)
-- 5. 执行第七部分数据迁移脚本(如需从旧表迁移)
-- 6. 验证数据一致性
-- 7. 切换服务配置, 启用分库分表
-- 8. 观察监控指标, 确认运行正常
-- ============================================================
