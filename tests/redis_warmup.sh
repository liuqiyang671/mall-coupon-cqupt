#!/bin/bash
# ============================================================
# Redis 缓存预热脚本
# 将优惠券模板信息写入 Redis，供 Lua 脚本使用
# 执行方式: bash redis_warmup.sh
# ============================================================

REDIS_CLI="redis-cli -h 127.0.0.1 -p 6379 -a Lqy259931"

echo '=== 清除旧的压测模板缓存 ==='
$REDIS_CLI DEL "one-coupon_engine:template:910000000000999"

echo '=== 写入优惠券模板缓存 (Hash) ==='
$REDIS_CLI HMSET "one-coupon_engine:template:910000000000999" \
  "id" "910000000000999" \
  "name" "JMeter压测专用券-立减10元" \
  "shopNumber" "1810714735922956666" \
  "source" "0" \
  "target" "1" \
  "goods" "" \
  "type" "0" \
  "validStartTime" "2026-01-01 00:00:00" \
  "validEndTime" "2027-12-31 23:59:59" \
  "stock" "500000" \
  "receiveRule" '{"limitPerPerson":500000,"usageInstructions":"JMeter压测专用券","distributionMode":"RECEIVE","applicableMerchantScope":"SPECIFIED","applicableShopNumbers":"1810714735922956666"}' \
  "consumeRule" '{"termsOfUse":0,"thresholdAmount":0,"maximumDiscountAmount":10.00,"maxDiscountAmount":10.00,"discountAmount":10.00,"explanationOfUnmetConditions":"当前订单暂不可用"}' \
  "status" "0"

echo '=== 验证缓存 ==='
$REDIS_CLI HGETALL "one-coupon_engine:template:910000000000999"

echo '=== 清除旧的用户领取记录 ==='
for i in $(seq 30001 33000); do
  $REDIS_CLI DEL "one-coupon_engine:user-template-limit:${i}_910000000000999"
done

echo '=== Redis 预热完成 ==='
$REDIS_CLI HGET "one-coupon_engine:template:910000000000999" stock