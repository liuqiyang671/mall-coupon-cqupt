"""
生成 JMeter 压测所需的全部测试数据：
1. SQL 文件（500 个测试用户 + 1 个高库存优惠券模板）
2. Redis 预热命令（缓存优惠券模板到 Redis）
3. JMeter CSV 文件（用户登录凭据）
4. 数据清理 SQL
"""
import os
import hashlib
import secrets

OUTPUT_DIR = os.path.dirname(os.path.abspath(__file__))

# BCrypt 生成（纯 Python 实现，不依赖 bcrypt 库）
def _bcrypt_encode(password: str) -> str:
    """使用已知的 BCrypt hash 或通过 subprocess 调用。"""
    try:
        import subprocess
        result = subprocess.run(
            ['java', '-cp', '.', '-e',
             f'import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; '
             f'System.out.println(new BCryptPasswordEncoder().encode("{password}"));'],
            capture_output=True, text=True, timeout=10
        )
        if result.returncode == 0 and result.stdout.strip():
            return result.stdout.strip()
    except Exception:
        pass
    return None

# 已知的 BCrypt hash（密码: Test123456），来自 frontend-test-data.sql
KNOWN_BCRYPT_HASH = '$2a$10$7KATYIo.cd7e6n4W7Y.o0.Db/nDxEpNPSn.uWrFAnXyyS7jeuhsHq'

# 测试配置
TEST_USER_COUNT = 3000
TEST_USER_START_ID = 30001
TEST_PASSWORD = 'Test123456'
TEST_TEMPLATE_ID = 910000000000999
TEST_SHOP_NUMBER = 1810714735922956666  # 已知路由到 ds_1.t_coupon_template_15
TEST_TEMPLATE_STOCK = 500000
LIMIT_PER_PERSON = 500000

# ============================================================
# 1. 生成 SQL
# ============================================================
def generate_sql():
    lines = []
    lines.append("-- ============================================================")
    lines.append("-- JMeter 压测测试数据 - 优惠券领取接口")
    lines.append("-- 生成时间: 自动生成")
    lines.append("-- 测试用户数: {}".format(TEST_USER_COUNT))
    lines.append("-- 测试模板ID: {}".format(TEST_TEMPLATE_ID))
    lines.append("-- 测试模板库存: {}".format(TEST_TEMPLATE_STOCK))
    lines.append("-- ============================================================")
    lines.append("")
    lines.append("SET NAMES utf8mb4;")
    lines.append("SET FOREIGN_KEY_CHECKS = 0;")
    lines.append("")

    # --- 测试用户 ---
    lines.append("-- ============================================================")
    lines.append("-- 第一部分: 500 个测试用户 (role_type=2, 普通用户)")
    lines.append("-- 用户名: jmeter_user_001 ~ jmeter_user_500")
    lines.append("-- 密码: {}".format(TEST_PASSWORD))
    lines.append("-- ============================================================")
    lines.append("")
    lines.append("USE mall_coupon_cqupt_0;")
    lines.append("")

    lines.append("-- 清理旧的测试用户")
    lines.append("DELETE FROM t_user WHERE id BETWEEN {} AND {};".format(
        TEST_USER_START_ID, TEST_USER_START_ID + TEST_USER_COUNT - 1))
    lines.append("")

    lines.append("INSERT INTO t_user")
    lines.append("(id, role_type, shop_number, username, password, nickname, real_name, phone, mail, status, activation_status, create_time, update_time, del_flag)")
    lines.append("VALUES")

    values = []
    for i in range(TEST_USER_COUNT):
        uid = TEST_USER_START_ID + i
        seq = str(i + 1).zfill(3)
        username = f'jmeter_user_{seq}'
        nickname = f'压测用户{seq}'
        phone = f'1380000{str(i + 1).zfill(4)}'
        mail = f'jmeter{seq}@test.local'
        values.append(
            f"({uid}, 2, NULL, '{username}', '{KNOWN_BCRYPT_HASH}', "
            f"'{nickname}', '压测用户', '{phone}', '{mail}', "
            f"0, 1, NOW(), NOW(), 0)"
        )

    # 分批插入，每批 100 条
    batch_size = 100
    for batch_start in range(0, len(values), batch_size):
        batch = values[batch_start:batch_start + batch_size]
        if batch_start > 0:
            lines.append("")
            lines.append("INSERT INTO t_user")
            lines.append("(id, role_type, shop_number, username, password, nickname, real_name, phone, mail, status, activation_status, create_time, update_time, del_flag)")
            lines.append("VALUES")
        lines.append(",\n".join(batch) + ";")

    lines.append("")

    # --- 优惠券模板 ---
    lines.append("-- ============================================================")
    lines.append("-- 第二部分: 压测专用优惠券模板")
    lines.append("-- 模板ID: {}".format(TEST_TEMPLATE_ID))
    lines.append("-- 店铺编号: {} (路由到 ds_1.t_coupon_template_15)".format(TEST_SHOP_NUMBER))
    lines.append("-- 库存: {}".format(TEST_TEMPLATE_STOCK))
    lines.append("-- 每人限领: {} 次".format(LIMIT_PER_PERSON))
    lines.append("-- 有效期: 2026-01-01 ~ 2027-12-31")
    lines.append("-- ============================================================")
    lines.append("")
    lines.append("USE mall_coupon_cqupt_1;")
    lines.append("")

    lines.append("-- 清理旧的压测模板")
    lines.append("DELETE FROM t_coupon_template_15 WHERE id = {};".format(TEST_TEMPLATE_ID))
    lines.append("")

    receive_rule = '{"limitPerPerson": %d, "usageInstructions": "JMeter压测专用券", "distributionMode": "RECEIVE", "applicableMerchantScope": "SPECIFIED", "applicableShopNumbers": "%s"}' % (LIMIT_PER_PERSON, TEST_SHOP_NUMBER)
    consume_rule = '{"termsOfUse": 0, "thresholdAmount": 0, "maximumDiscountAmount": 10.00, "maxDiscountAmount": 10.00, "discountAmount": 10.00, "explanationOfUnmetConditions": "当前订单暂不可用"}'

    lines.append("INSERT INTO t_coupon_template_15")
    lines.append("(id, name, shop_number, source, target, goods, type, valid_start_time, valid_end_time, stock, receive_rule, consume_rule, status, create_time, update_time, del_flag)")
    lines.append("VALUES")
    lines.append("({}, 'JMeter压测专用券-立减10元', {}, 0, 1, NULL, 0, '2026-01-01 00:00:00', '2027-12-31 23:59:59', {}, '{}', '{}', 0, NOW(), NOW(), 0);".format(
        TEST_TEMPLATE_ID, TEST_SHOP_NUMBER, TEST_TEMPLATE_STOCK, receive_rule, consume_rule))

    lines.append("")
    lines.append("SET FOREIGN_KEY_CHECKS = 1;")
    lines.append("")
    lines.append("-- 验证插入结果")
    lines.append("SELECT COUNT(*) AS user_count FROM mall_coupon_cqupt_0.t_user WHERE id BETWEEN {} AND {};".format(
        TEST_USER_START_ID, TEST_USER_START_ID + TEST_USER_COUNT - 1))
    lines.append("SELECT id, name, stock, valid_start_time, valid_end_time FROM mall_coupon_cqupt_1.t_coupon_template_15 WHERE id = {};".format(TEST_TEMPLATE_ID))

    return "\n".join(lines)


# ============================================================
# 2. 生成 Redis 预热脚本
# ============================================================
def generate_redis_script():
    lines = []
    lines.append("#!/bin/bash")
    lines.append("# ============================================================")
    lines.append("# Redis 缓存预热脚本")
    lines.append("# 将优惠券模板信息写入 Redis，供 Lua 脚本使用")
    lines.append("# 执行方式: bash redis_warmup.sh")
    lines.append("# ============================================================")
    lines.append("")
    lines.append("REDIS_CLI=\"redis-cli -h 127.0.0.1 -p 6379 -a Lqy259931\"")
    lines.append("")
    lines.append("echo '=== 清除旧的压测模板缓存 ==='")
    lines.append("$REDIS_CLI DEL \"one-coupon_engine:template:{}\"".format(TEST_TEMPLATE_ID))
    lines.append("")
    lines.append("echo '=== 写入优惠券模板缓存 (Hash) ==='")
    lines.append("$REDIS_CLI HMSET \"one-coupon_engine:template:{}\" \\".format(TEST_TEMPLATE_ID))
    lines.append("  \"id\" \"{}\" \\".format(TEST_TEMPLATE_ID))
    lines.append("  \"name\" \"JMeter压测专用券-立减10元\" \\")
    lines.append("  \"shopNumber\" \"{}\" \\".format(TEST_SHOP_NUMBER))
    lines.append("  \"source\" \"0\" \\")
    lines.append("  \"target\" \"1\" \\")
    lines.append("  \"goods\" \"\" \\")
    lines.append("  \"type\" \"0\" \\")
    lines.append("  \"validStartTime\" \"2026-01-01 00:00:00\" \\")
    lines.append("  \"validEndTime\" \"2027-12-31 23:59:59\" \\")
    lines.append("  \"stock\" \"{}\" \\".format(TEST_TEMPLATE_STOCK))
    lines.append("  \"receiveRule\" '{\"limitPerPerson\":%d,\"usageInstructions\":\"JMeter压测专用券\",\"distributionMode\":\"RECEIVE\",\"applicableMerchantScope\":\"SPECIFIED\",\"applicableShopNumbers\":\"%s\"}' \\" % (LIMIT_PER_PERSON, TEST_SHOP_NUMBER))
    lines.append("  \"consumeRule\" '{\"termsOfUse\":0,\"thresholdAmount\":0,\"maximumDiscountAmount\":10.00,\"maxDiscountAmount\":10.00,\"discountAmount\":10.00,\"explanationOfUnmetConditions\":\"当前订单暂不可用\"}' \\")
    lines.append("  \"status\" \"0\"")
    lines.append("")
    lines.append("echo '=== 验证缓存 ==='")
    lines.append("$REDIS_CLI HGETALL \"one-coupon_engine:template:{}\"".format(TEST_TEMPLATE_ID))
    lines.append("")
    lines.append("echo '=== 清除旧的用户领取记录 ==='")
    lines.append("for i in $(seq {} {}); do".format(TEST_USER_START_ID, TEST_USER_START_ID + TEST_USER_COUNT - 1))
    lines.append("  $REDIS_CLI DEL \"one-coupon_engine:user-template-limit:${{i}}_{}\"".format(TEST_TEMPLATE_ID))
    lines.append("done")
    lines.append("")
    lines.append("echo '=== Redis 预热完成 ==='")
    lines.append("$REDIS_CLI HGET \"one-coupon_engine:template:{}\" stock".format(TEST_TEMPLATE_ID))

    return "\n".join(lines)


# ============================================================
# 3. 生成 JMeter CSV
# ============================================================
def generate_jmeter_csv():
    lines = ["username,password,roleType"]
    for i in range(TEST_USER_COUNT):
        seq = str(i + 1).zfill(3)
        lines.append(f"jmeter_user_{seq},{TEST_PASSWORD},2")
    return "\n".join(lines)


# ============================================================
# 4. 生成数据清理 SQL
# ============================================================
def generate_cleanup_sql():
    lines = []
    lines.append("-- ============================================================")
    lines.append("-- 清理 JMeter 压测数据")
    lines.append("-- ============================================================")
    lines.append("")
    lines.append("SET FOREIGN_KEY_CHECKS = 0;")
    lines.append("")
    lines.append("-- 清理测试用户")
    lines.append("DELETE FROM mall_coupon_cqupt_0.t_user WHERE id BETWEEN {} AND {};".format(
        TEST_USER_START_ID, TEST_USER_START_ID + TEST_USER_COUNT - 1))
    lines.append("")
    lines.append("-- 清理压测优惠券模板")
    lines.append("DELETE FROM mall_coupon_cqupt_1.t_coupon_template_15 WHERE id = {};".format(TEST_TEMPLATE_ID))
    lines.append("")
    lines.append("-- 清理测试用户领取的优惠券（分片表，需逐表清理）")
    for i in range(32):
        lines.append("DELETE FROM mall_coupon_cqupt_{}.t_user_coupon_{} WHERE coupon_template_id = {};".format(
            0 if i < 16 else 1, i, TEST_TEMPLATE_ID))
    lines.append("")
    lines.append("SET FOREIGN_KEY_CHECKS = 1;")
    lines.append("")
    lines.append("SELECT '清理完成' AS status;")

    return "\n".join(lines)


# ============================================================
# 5. 生成验证 SQL
# ============================================================
def generate_verify_sql():
    lines = []
    lines.append("-- ============================================================")
    lines.append("-- 压测前数据验证")
    lines.append("-- 在执行压测前运行此脚本，确认数据准备就绪")
    lines.append("-- ============================================================")
    lines.append("")
    lines.append("-- 1. 验证测试用户数量")
    lines.append("SELECT COUNT(*) AS test_user_count FROM mall_coupon_cqupt_0.t_user")
    lines.append("WHERE id BETWEEN {} AND {};".format(
        TEST_USER_START_ID, TEST_USER_START_ID + TEST_USER_COUNT - 1))
    lines.append("-- 预期: {}".format(TEST_USER_COUNT))
    lines.append("")
    lines.append("-- 2. 验证优惠券模板")
    lines.append("SELECT id, name, stock, status, valid_start_time, valid_end_time")
    lines.append("FROM mall_coupon_cqupt_1.t_coupon_template_15")
    lines.append("WHERE id = {};".format(TEST_TEMPLATE_ID))
    lines.append("-- 预期: 1 条记录，stock={}".format(TEST_TEMPLATE_STOCK))
    lines.append("")
    lines.append("-- 3. 验证无历史领取记录")
    lines.append("SELECT COUNT(*) AS existing_coupons FROM mall_coupon_cqupt_1.t_user_coupon_19")
    lines.append("WHERE coupon_template_id = {};".format(TEST_TEMPLATE_ID))
    lines.append("-- 预期: 0")
    lines.append("")
    lines.append("-- 4. 验证用户可登录（抽样检查）")
    lines.append("SELECT id, username, status, activation_status")
    lines.append("FROM mall_coupon_cqupt_0.t_user")
    lines.append("WHERE id IN ({}, {}, {});".format(
        TEST_USER_START_ID,
        TEST_USER_START_ID + 249,
        TEST_USER_START_ID + 499))

    return "\n".join(lines)


# ============================================================
# 主程序
# ============================================================
if __name__ == '__main__':
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    # 1. SQL
    sql_path = os.path.join(OUTPUT_DIR, 'jmeter_test_data.sql')
    with open(sql_path, 'w', encoding='utf-8') as f:
        f.write(generate_sql())
    print(f'[OK] SQL 文件: {sql_path}')

    # 2. Redis
    redis_path = os.path.join(OUTPUT_DIR, 'redis_warmup.sh')
    with open(redis_path, 'w', encoding='utf-8') as f:
        f.write(generate_redis_script())
    print(f'[OK] Redis 预热脚本: {redis_path}')

    # 3. CSV
    csv_path = os.path.join(OUTPUT_DIR, 'jmeter_users.csv')
    with open(csv_path, 'w', encoding='utf-8') as f:
        f.write(generate_jmeter_csv())
    print(f'[OK] JMeter CSV: {csv_path}')

    # 4. 清理
    cleanup_path = os.path.join(OUTPUT_DIR, 'jmeter_cleanup.sql')
    with open(cleanup_path, 'w', encoding='utf-8') as f:
        f.write(generate_cleanup_sql())
    print(f'[OK] 清理 SQL: {cleanup_path}')

    # 5. 验证
    verify_path = os.path.join(OUTPUT_DIR, 'jmeter_verify.sql')
    with open(verify_path, 'w', encoding='utf-8') as f:
        f.write(generate_verify_sql())
    print(f'[OK] 验证 SQL: {verify_path}')

    print(f'\n=== 生成完成 ===')
    print(f'测试用户: {TEST_USER_COUNT} 个 (ID: {TEST_USER_START_ID} ~ {TEST_USER_START_ID + TEST_USER_COUNT - 1})')
    print(f'优惠券模板ID: {TEST_TEMPLATE_ID}')
    print(f'模板库存: {TEST_TEMPLATE_STOCK}')
    print(f'每人限领: {LIMIT_PER_PERSON} 次')
    print(f'店铺编号: {TEST_SHOP_NUMBER} (路由到 ds_1.t_coupon_template_15)')
