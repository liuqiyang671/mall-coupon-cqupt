# 核心风险矩阵

> 项目：OneCoupon 优惠券商城  
> 范围：P0 / P1 核心链路、风险点、已补测试与剩余缺口  
> 更新日期：2026-05-09

## 1. 风险分级标准

| 等级 | 定义 | 示例 |
| --- | --- | --- |
| P0 | 影响核心交易、金额、鉴权、库存一致性或系统可用性 | 登录绕过、超发券、结算金额错误、支付用券状态错误 |
| P1 | 影响重要业务链路、异步任务、数据同步、公共框架能力或主要用户体验 | 批量发券失败、消息重复消费、搜索不同步、缓存不一致 |
| P2 | 辅助功能或可人工修复的问题 | 非核心筛选异常、提示文案不准、辅助工具不可用 |

本文只列 P0 / P1，作为测试开发近期建设重点。

## 2. P0 核心链路

| 链路 | 涉及模块 | 当前测试状态 | 核心风险 | 下一步 |
| --- | --- | --- | --- | --- |
| 登录鉴权 | `merchant-admin`, `gateway`, `framework` | `UserServiceImplTest`, `JWTUtilTest`, `TokenValidateFilterTest` 已覆盖主要单测 | Token 失效仍可访问、Header 身份错乱、商家越权 | 增加 Redis Token TTL 与接口集成测试 |
| 优惠券模板创建 | `merchant-admin` | 参数过滤器与 Service 单测已补 | 错误券规则上线、平台券/商家券越权、缓存状态不一致 | 增加 DB + Redis 一致性集成测试和日志断言 |
| 商品管理 | `merchant-admin` | 商品、分类、属性主要分支已补 | 商品状态错误、库存调整失败、上架商品误删 | 增加 Controller/API 测试和库存并发测试 |
| 购物车 | `engine` | 加购、合并、数量、汇总金额、税费、下架兜底已补 | 购物车金额错误、下架商品参与结算、跨用户数据串号 | 增加接口层与前端 E2E |
| 可用券查询 | `settlement` | 优惠计算策略、同步可用券分类已补 | 可用券判断错误、优惠金额错误、商品券不匹配 NPE | 补并行版本一致性测试和更多金额精度用例 |
| 应用指定优惠券 | `settlement` | 当前仅有 `@Disabled` 待实现测试 | `CouponApplyServiceImpl.applySelectedCoupon` 仍直接返回 `null`，交易闭环阻塞 | 先实现业务逻辑，再启用测试作为回归 |
| 支付用券/核销/退款 | `engine` | 锁、金额校验、结算单创建、锁券、核销失败、退款恢复已补 | 重复核销、退款后券状态错误、金额被篡改 | 增加真实事务集成测试和重复回调幂等测试 |
| 同步领券 | `engine` | 仅工具解析与部分用户券查询被覆盖 | Redis Lua、DB 落库、用户券缓存一致性仍有风险 | 补 Redis Lua 集成测试、并发领券测试 |
| 异步领券 | `engine`, `distribution` | MQ 业务链路未覆盖 | 预扣库存成功但用户券未落库、消息重复导致重复发券 | 补 Producer/Consumer 单测与 MQ 集成测试 |

## 3. P1 核心链路

| 链路 | 涉及模块 | 当前测试状态 | 核心风险 | 下一步 |
| --- | --- | --- | --- | --- |
| 批量发券任务 | `merchant-admin`, `distribution` | 分发策略选择已补，任务链路未补 | 漏发、重复发、任务状态不可追踪 | 补 Excel 解析、任务状态、Consumer 测试 |
| 提醒预约 | `engine`, `distribution` | bitmap 工具已补，服务链路未补 | 重复提醒、取消后仍提醒、延迟消息失败 | 补创建/取消提醒 Service 与延迟消息测试 |
| 搜索同步 | `search` | 仅 ES 连接冒烟 | 优惠券模板搜索结果与 DB 不一致 | 补 Canal/MQ Consumer 与索引断言 |
| 分库分表路由 | 多后端模块 | 仅有建表 SQL 工具 | 写错分片、查询不到数据、全路由扫描 | 用 Testcontainers 或集成环境补路由断言 |
| Redis 缓存一致性 | `merchant-admin`, `engine`, `settlement` | 单测覆盖部分缓存写入/读取 | DB 与 Redis 状态不一致、缓存穿透、缓存丢失 | 补 DB + Redis 对账和降级测试 |
| 幂等切面 | `framework` | SpEL 基础解析已补，切面未补 | 重复提交、重复消费污染业务数据 | 补 `NoDuplicateSubmitAspectTest`, `NoMQDuplicateConsumeAspectTest` |
| 统一异常响应 | `framework` | `ResultsTest` 已补，异常处理器未补 | 接口错误格式不统一，前端难处理 | 补 `GlobalExceptionHandlerTest` |
| 前端关键流程 | `frontend` | 缺失 | 登录、领券、购物车、结算页面不可用或金额显示错误 | 建议 Playwright E2E |

## 4. 已降低的风险

| 风险 | 原状态 | 当前状态 |
| --- | --- | --- |
| 网关鉴权完全无测试 | 无白名单、非法 Token、Header 透传测试 | 已补 `TokenValidateFilterTest` |
| 结算模块完全无测试 | 优惠计算和可用券查询无保护 | 已补策略和查询服务单测 |
| 商品相关业务无测试 | 分类、属性、商品创建/库存/状态无保护 | 已补 Service 单测 |
| 优惠券模板校验规则存在绕过 | Integer 与枚举对象比较导致规则不生效 | 已修复并用测试锁定 |
| 商品券不匹配可能 NPE | 不可用后仍继续解引用 | 已修复并用测试覆盖 |
| 分发策略无测试 | 策略注册/重复 mark/未知 mark 无保护 | 已补 `DistributionStrategyChooseTest` |

## 5. 回归建议

| 修改范围 | 必跑测试 |
| --- | --- |
| `gateway/filter` | `TokenValidateFilterTest` |
| `merchant-admin` 用户认证 | `UserServiceImplTest`, `JWTUtilTest`, `PasswordEncoderTest` |
| `merchant-admin` 优惠券模板 | `CouponTemplateCreateParamFilterTest`, `CouponTemplateServiceImplTest` |
| `merchant-admin` 商品域 | `GoodsServiceImplTest`, `GoodsCategoryServiceImplTest`, `GoodsAttributeServiceImplTest` |
| `engine` 购物车 | `CartServiceImplTest` |
| `engine` 支付用券 | `CouponPayServiceImplTest` |
| `engine` 用户券 | `UserCouponServiceImplTest` |
| `settlement` | `CouponCalculationStrategyTest`, `CouponQueryServiceImplTest` |
| `distribution` | `DistributionStrategyChooseTest`, `SendMessageMarkCovertEnumTest` |
| `framework` | `ResultsTest`, `SpELUtilTest` |

## 6. 阻塞项

1. `CouponApplyServiceImpl.applySelectedCoupon` 未实现，无法补真实成功/失败单测。当前已保留 `@Disabled` 测试作为明确缺口。
2. 真实 Redis Lua、RocketMQ、Elasticsearch 和分库分表路由不是纯单元测试能可靠覆盖的内容，需要单独建设集成测试 Profile。
3. 现有部分测试包含真实外部地址、无限等待或造数行为，不适合默认 `mvn test` 全量执行。

