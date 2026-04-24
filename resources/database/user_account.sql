CREATE DATABASE IF NOT EXISTS mall_coupon_cqupt_0;
CREATE DATABASE IF NOT EXISTS mall_coupon_cqupt_1;

USE mall_coupon_cqupt_0;

CREATE TABLE IF NOT EXISTS `t_user`
(
    `id`                bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `role_type`         tinyint(1) NOT NULL DEFAULT 1 COMMENT '用户角色 0：平台人员 1：商家 2：普通用户',
    `shop_number`       varchar(64)  DEFAULT NULL COMMENT '店铺编号，仅商家角色需要',
    `username`          varchar(64)  NOT NULL COMMENT '用户名',
    `password`          varchar(512) NOT NULL COMMENT 'BCrypt 加密密码',
    `nickname`          varchar(64)  DEFAULT NULL COMMENT '昵称',
    `real_name`         varchar(64)  DEFAULT NULL COMMENT '真实姓名',
    `phone`             varchar(128) DEFAULT NULL COMMENT '手机号',
    `mail`              varchar(512) DEFAULT NULL COMMENT '邮箱',
    `avatar_url`        varchar(512) DEFAULT NULL COMMENT '头像地址',
    `status`            tinyint(1) NOT NULL DEFAULT 0 COMMENT '账号状态 0：正常 1：禁用',
    `activation_status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '激活状态 0：未激活 1：已激活',
    `last_login_time`   datetime     DEFAULT NULL COMMENT '最后登录时间',
    `create_time`       datetime     DEFAULT NULL COMMENT '创建时间',
    `update_time`       datetime     DEFAULT NULL COMMENT '修改时间',
    `del_flag`          tinyint(1) DEFAULT 0 COMMENT '删除标识 0：未删除 1：已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_username` (`role_type`, `username`),
    UNIQUE KEY `uk_role_phone` (`role_type`, `phone`),
    UNIQUE KEY `uk_role_mail` (`role_type`, `mail`),
    KEY                 `idx_role_status` (`role_type`, `status`, `activation_status`),
    KEY                 `idx_shop_number` (`shop_number`),
    CONSTRAINT `chk_user_role_type` CHECK (`role_type` IN (0, 1, 2)),
    CONSTRAINT `chk_user_status` CHECK (`status` IN (0, 1)),
    CONSTRAINT `chk_user_activation_status` CHECK (`activation_status` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统统一用户账号表';

-- Existing databases created before the three-role account model should migrate the old t_user
-- structure manually before adding the unique indexes above. Keep the migration explicit in
-- release scripts so duplicate columns or indexes can be handled according to the target MySQL version.
