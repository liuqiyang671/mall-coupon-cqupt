-- ============================================================
-- 清理 JMeter 压测数据
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 清理测试用户
DELETE FROM mall_coupon_cqupt_0.t_user WHERE id BETWEEN 30001 AND 33000;

-- 清理压测优惠券模板
DELETE FROM mall_coupon_cqupt_1.t_coupon_template_15 WHERE id = 910000000000999;

-- 清理测试用户领取的优惠券（分片表，需逐表清理）
DELETE FROM mall_coupon_cqupt_0.t_user_coupon_0 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_0.t_user_coupon_1 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_0.t_user_coupon_2 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_0.t_user_coupon_3 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_0.t_user_coupon_4 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_0.t_user_coupon_5 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_0.t_user_coupon_6 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_0.t_user_coupon_7 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_0.t_user_coupon_8 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_0.t_user_coupon_9 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_0.t_user_coupon_10 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_0.t_user_coupon_11 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_0.t_user_coupon_12 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_0.t_user_coupon_13 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_0.t_user_coupon_14 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_0.t_user_coupon_15 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_1.t_user_coupon_16 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_1.t_user_coupon_17 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_1.t_user_coupon_18 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_1.t_user_coupon_19 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_1.t_user_coupon_20 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_1.t_user_coupon_21 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_1.t_user_coupon_22 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_1.t_user_coupon_23 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_1.t_user_coupon_24 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_1.t_user_coupon_25 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_1.t_user_coupon_26 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_1.t_user_coupon_27 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_1.t_user_coupon_28 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_1.t_user_coupon_29 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_1.t_user_coupon_30 WHERE coupon_template_id = 910000000000999;
DELETE FROM mall_coupon_cqupt_1.t_user_coupon_31 WHERE coupon_template_id = 910000000000999;

SET FOREIGN_KEY_CHECKS = 1;

SELECT '清理完成' AS status;