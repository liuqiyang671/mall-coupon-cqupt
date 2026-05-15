# JMeter 压测测试数据准备说明

## 生成的文件

| 文件 | 说明 | 执行顺序 |
|---|---|---|
| `jmeter_test_data.sql` | 数据库测试数据（3000用户 + 1个优惠券模板） | ① |
| `redis_warmup.sh` | Redis 缓存预热（模板信息写入 Redis） | ② |
| `jmeter_verify.sql` | 压测前数据验证 | ③ |
| `jmeter_users.csv` | JMeter CSV 数据文件（用户登录凭据） | 用于 JMeter |
| `jmeter_cleanup.sql` | 压测结束后清理数据 | 最后执行 |

---

## 一、执行步骤

### 步骤 1：执行数据库 SQL

```bash
mysql -u root -pLqy259931 < tests/jmeter_test_data.sql
```

执行内容：
- 在 `mall_coupon_cqupt_0.t_user` 插入 3000 个测试用户（ID: 30001~33000）
- 在 `mall_coupon_cqupt_1.t_coupon_template_15` 插入 1 个压测专用优惠券模板（ID: 910000000000999）

### 步骤 2：Redis 缓存预热

```bash
bash tests/redis_warmup.sh
```

执行内容：
- 将优惠券模板信息写入 Redis Hash（Lua 脚本依赖此缓存）
- 清除旧的用户领取记录

> **重要：** 这一步必须执行，否则 Lua 脚本会因读不到 stock 而直接返回库存不足。

### 步骤 3：验证数据

```bash
mysql -u root -pLqy259931 < tests/jmeter_verify.sql
```

确认：
- 测试用户数量 = 500
- 优惠券模板存在且 stock = 500000
- 无历史领取记录

### 步骤 4：配置 JMeter

在 JMeter 的 CSV Data Set Config 中引用 `jmeter_users.csv`：

| 配置项 | 值 |
|---|---|
| Filename | `tests/jmeter_users.csv` |
| Variable Names | `username,password,roleType` |
| Delimiter | `,` |
| Recycle on EOF | False |
| Stop Thread on EOF | True |
| Sharing Mode | Current thread group |

登录请求 Body 使用变量：

```json
{
  "username": "${username}",
  "password": "${password}",
  "roleType": ${roleType}
}
```

### 步骤 5：压测结束后清理

```bash
mysql -u root -pLqy259931 < tests/jmeter_cleanup.sql
```

---

## 二、测试数据详情

### 2.1 测试用户

| 属性 | 值 |
|---|---|
| 数量 | 3000 |
| ID 范围 | 30001 ~ 33000 |
| 用户名 | jmeter_user_001 ~ jmeter_user_3000 |
| 密码 | Test123456 |
| 角色 | role_type=2（普通用户） |
| 状态 | status=0, activation_status=1（正常激活） |

### 2.2 优惠券模板

| 属性 | 值 |
|---|---|
| 模板 ID | 910000000000999 |
| 名称 | JMeter压测专用券-立减10元 |
| 店铺编号 | 1810714735922956666 |
| 类型 | type=0（固定金额券） |
| 优惠金额 | 10 元 |
| 库存 | 500000 |
| 每人限领 | 500000 次 |
| 有效期 | 2026-01-01 ~ 2027-12-31 |
| 状态 | status=0（进行中） |
| 分片位置 | ds_1.t_coupon_template_15 |

### 2.3 Redis 缓存 Key

| Key | 类型 | 说明 |
|---|---|---|
| `one-coupon_engine:template:910000000000999` | Hash | 优惠券模板缓存（Lua 脚本读取 stock） |
| `one-coupon_engine:user-template-limit:{userId}_910000000000999` | String | 用户领取次数（Lua 脚本判断限领） |

---

## 三、JMeter 测试计划配置

### 3.1 线程组结构

```
测试计划
├── 线程组1: 登录获取Token (3000线程, Ramp-Up 30s, 1次)
│   ├── CSV Data Set Config (jmeter_users.csv)
│   ├── HTTP 请求: POST /api/merchant-admin/user/login
│   │   └── JSON 提取器: token = $.data.token
│   └── 调试取样器
│
├── 线程组2: 同步领取 (3000线程, Ramp-Up 10s, 1次)
│   ├── HTTP Header Manager (Authorization: Bearer ${token})
│   └── HTTP 请求: POST /api/engine/user-coupon/redeem
│       └── Body: {"source":0, "shopNumber":"1810714735922956666", "couponTemplateId":"910000000000999"}
│
└── 线程组3: MQ异步领取 (3000线程, Ramp-Up 10s, 1次)
    ├── HTTP Header Manager (Authorization: Bearer ${token})
    └── HTTP 请求: POST /api/engine/user-coupon/redeem-mq
        └── Body: {"source":0, "shopNumber":"1810714735922956666", "couponTemplateId":"910000000000999"}
```

### 3.2 请求详情

**登录请求：**
- URL: `POST http://localhost:10000/api/merchant-admin/user/login`
- Header: `Content-Type: application/json`
- Body: `{"username":"${username}","password":"${password}","roleType":${roleType}}`

**同步领取请求：**
- URL: `POST http://localhost:10000/api/engine/user-coupon/redeem`
- Header: `Authorization: Bearer ${token}`, `Content-Type: application/json`
- Body: `{"source":0,"shopNumber":"1810714735922956666","couponTemplateId":"910000000000999"}`

**MQ 异步领取请求：**
- URL: `POST http://localhost:10000/api/engine/user-coupon/redeem-mq`
- Header: `Authorization: Bearer ${token}`, `Content-Type: application/json`
- Body: `{"source":0,"shopNumber":"1810714735922956666","couponTemplateId":"910000000000999"}`

---

## 四、压测后验证

压测结束后执行以下 SQL 验证数据一致性：

```sql
-- 1. 数据库库存（不应为负数）
SELECT id, name, stock 
FROM mall_coupon_cqupt_1.t_coupon_template_15 
WHERE id = 910000000000999;
-- 预期: stock = 500000 - 成功领取数

-- 2. 用户优惠券记录数
SELECT COUNT(*) AS total_received
FROM mall_coupon_cqupt_1.t_user_coupon_19
WHERE coupon_template_id = 910000000000999;
-- 预期: 等于成功请求数（最多3000）

-- 3. 检查无重复领取
SELECT user_id, COUNT(*) AS cnt
FROM mall_coupon_cqupt_1.t_user_coupon_19
WHERE coupon_template_id = 910000000000999
GROUP BY user_id
HAVING cnt > 1;
-- 预期: 0 条（无重复）

-- 4. Redis 库存
-- redis-cli HGET one-coupon_engine:template:910000000000999 stock
-- 预期: 与数据库 stock 一致
```

---

## 五、注意事项

1. **必须先执行 Redis 预热**，否则 Lua 脚本读不到 stock 字段会返回库存不足
2. **每个用户限领 500000 次**（limitPerPerson=500000），不会出现领取上限的业务错误
3. **模板库存 500000** 远大于用户总领取次数，不会出现库存不足的业务错误
4. **清理数据时**会删除所有该模板的领取记录（包括非压测数据），请确认无影响
5. **MQ 异步接口**的领取结果有几秒延迟，压测后等待 10 秒再查数据库
