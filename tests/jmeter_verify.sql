-- ============================================================
-- 压测前数据验证
-- 在执行压测前运行此脚本，确认数据准备就绪
-- ============================================================

-- 1. 验证测试用户数量
SELECT COUNT(*) AS test_user_count FROM mall_coupon_cqupt_0.t_user
WHERE id BETWEEN 30001 AND 33000;
-- 预期: 3000

-- 2. 验证优惠券模板
SELECT id, name, stock, status, valid_start_time, valid_end_time
FROM mall_coupon_cqupt_1.t_coupon_template_15
WHERE id = 910000000000999;
-- 预期: 1 条记录，stock=500000

-- 3. 验证无历史领取记录
SELECT COUNT(*) AS existing_coupons FROM mall_coupon_cqupt_1.t_user_coupon_19
WHERE coupon_template_id = 910000000000999;
-- 预期: 0

-- 4. 验证用户可登录（抽样检查）
SELECT id, username, status, activation_status
FROM mall_coupon_cqupt_0.t_user
WHERE id IN (30001, 30250, 30500);