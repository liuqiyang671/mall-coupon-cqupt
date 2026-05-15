# API 自动化回归测试指南

## 目标

本目录提供 Postman/Newman API 回归集合，用于覆盖商城后端的核心链路：

| 链路 | 覆盖接口 |
| --- | --- |
| 登录与鉴权 | 商家登录、普通用户登录、用户信息、未登录访问失败 |
| 商品管理 | 分类树、属性列表、创建商品、查询商品、商品详情、库存调整、上下架 |
| 券模板管理 | 创建券模板、分页查询、详情查询、增加库存 |
| 领券中心 | 查询可领券、领券、查询用户券 |
| 购物车 | 加购、购物车汇总、修改数量、选中商品 |
| 结算 | 查询可用券、应用优惠券 |

## 文件说明

| 文件 | 说明 |
| --- | --- |
| `tests/api/onecoupon.postman_collection.json` | Postman v2.1 集合，包含请求、变量提取和断言脚本。 |
| `tests/api/local.postman_environment.json` | 本地环境变量，默认直连各后端服务端口。 |
| `resources/database/frontend-test-data.sql` | API 回归依赖的用户、商品、购物车、优惠券测试数据。 |
| `resources/database/frontend-test-redis-warmup.redis` | 前端/API 回归依赖的 Redis 预热数据。 |

## 本地环境准备

默认集合使用直连服务端口：

| 服务 | 默认地址 |
| --- | --- |
| `merchant-admin` | `http://localhost:10010` |
| `engine` | `http://localhost:10020` |
| `settlement` | `http://localhost:10030` |
| `gateway` | `http://localhost:10000`，默认不使用 |

执行前请准备：

1. 启动 MySQL、Redis、RocketMQ 等后端运行依赖。
2. 初始化项目表结构后，导入 `resources/database/frontend-test-data.sql`。
3. 使用 `resources/database/frontend-test-redis-warmup.redis` 预热 Redis。
4. 启动 `merchant-admin`、`engine`、`settlement` 服务。

PowerShell 示例：

```powershell
redis-cli -h 127.0.0.1 -p 6379 < resources/database/frontend-test-redis-warmup.redis

mvn -pl merchant-admin -am spring-boot:run
mvn -pl engine -am spring-boot:run
mvn -pl settlement -am spring-boot:run
```

如果需要通过网关执行，可以把环境文件中的 `merchantAdminBaseUrl`、`engineBaseUrl`、`settlementBaseUrl` 改成 `{{gatewayBaseUrl}}`，同时确认网关路由、Nacos 注册和鉴权过滤器配置已经启动。

## Postman 使用方式

1. 打开 Postman。
2. 导入 `tests/api/onecoupon.postman_collection.json`。
3. 导入 `tests/api/local.postman_environment.json`。
4. 选择环境 `OneCoupon Local`。
5. 先单独运行 `Auth / Merchant Login` 和 `Auth / Customer Login`，确认 token 能写入环境变量。
6. 运行整个 `OneCoupon API Regression` 集合。

集合会自动串联以下动态变量：

| 变量 | 来源 |
| --- | --- |
| `merchantToken`、`merchantUserId` | 商家登录响应 |
| `customerToken`、`customerUserId` | 普通用户登录响应 |
| `createdGoodsName`、`createdGoodsId` | 创建商品和商品分页查询 |
| `createdCouponName`、`createdCouponTemplateId` | 创建券模板和券模板分页查询 |
| `cartId` | 购物车汇总响应，失败时回退到种子数据 |
| `applyCouponTemplateId` | 查询可用券响应，失败时回退到种子券模板 |
| `orderId` | 应用优惠券请求前动态生成 |

## Newman 执行方式

安装 Newman：

```bash
npm install -g newman
```

执行回归集合：

```bash
newman run tests/api/onecoupon.postman_collection.json -e tests/api/local.postman_environment.json
```

生成报告示例：

```bash
npm install -g newman-reporter-htmlextra
newman run tests/api/onecoupon.postman_collection.json \
  -e tests/api/local.postman_environment.json \
  -r cli,htmlextra \
  --reporter-htmlextra-export tests/api/reports/onecoupon-api.html
```

Windows PowerShell 单行版本：

```powershell
newman run tests/api/onecoupon.postman_collection.json -e tests/api/local.postman_environment.json -r cli,htmlextra --reporter-htmlextra-export tests/api/reports/onecoupon-api.html
```

## 断言策略

集合中每个请求至少包含：

- HTTP 状态断言。
- 统一响应 `code` 断言，成功值为环境变量 `successCode=0`。
- 核心 `data` 字段断言，例如 token、用户 ID、分页 records、购物车 totals、可用券列表。

注意：`/api/settlement/apply-coupon/{couponId}` 当前直接返回 `ApplyCouponRespDTO`，不是统一 `Result` 包装。因此该请求断言 `orderId`、`originalAmount`、`finalAmount`、`appliedCouponId`，不校验 `code`。

## 当前接口契约注意点

`settlement` 的应用优惠券接口路径变量名叫 `couponId`，但当前后端实现按 `couponTemplateId` 查询用户券：

```text
/api/settlement/apply-coupon/{{applyCouponTemplateId}}
```

因此集合使用 `applyCouponTemplateId`，优先来自“查询可用券”响应；如果响应为空，则回退到 `frontend-test-data.sql` 中的种子券模板 `910000000000002`。

另外，部分响应中存在超过 JavaScript 安全整数范围的雪花 ID。Postman 脚本对关键大 ID 使用响应文本正则提取，避免 `pm.response.json()` 解析大整数时丢精度。

## 常见失败原因

| 现象 | 常见原因 | 处理方式 |
| --- | --- | --- |
| `ECONNREFUSED` | 对应服务未启动或端口不一致 | 检查 `merchantAdminBaseUrl`、`engineBaseUrl`、`settlementBaseUrl`。 |
| 登录失败 | `frontend-test-data.sql` 未导入，或密码被改动 | 重新导入测试数据，确认账号 `merchant01`、`customer01` 和密码 `Test123456`。 |
| 用户信息返回 401 | 登录请求未先执行，或 Redis 中 token 会话丢失 | 重新运行两个登录请求。 |
| 商品、购物车接口数据为空 | MySQL 种子数据或 Redis 商品缓存未准备 | 导入 SQL 并执行 Redis 预热脚本。 |
| 领券失败 | Redis 券模板缓存缺失、RocketMQ 未启动、重复领取或库存不足 | 预热 Redis，启动 RocketMQ，必要时重新导入测试数据。 |
| 查询可用券为空 | 用户券数据缺失或结算入参与券规则不匹配 | 检查 `t_user_coupon_19` 种子数据和 `goodsList`。 |
| 应用优惠券失败 | 当前接口按券模板 ID 匹配用户券，不接受用户券实例 ID | 使用 `applyCouponTemplateId`，不要传 `userCoupon.id`。 |

## 剩余阻塞项

当前仓库只提供 API 集合结构、断言脚本和本地环境变量。完整端到端执行需要真实运行后端服务和中间件，尤其是 Redis、MySQL、RocketMQ。若这些依赖没有启动，Newman 会在网络连接、领券或缓存读取阶段失败，这是环境阻塞，不代表集合结构错误。
