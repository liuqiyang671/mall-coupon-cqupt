# 测试资产地图

> 项目：OneCoupon 优惠券商城  
> 范围：后端多模块测试资产、业务能力覆盖、缺失测试与覆盖矩阵  
> 更新日期：2026-05-09

## 1. 总览

本轮补齐后，后端测试相关 Java 文件从原来的 16 个提升到 35 个，其中新增 19 个以 Mockito/JUnit 为主的稳定单元测试文件。新增测试重点覆盖 P0/P1 后端业务逻辑：网关鉴权、优惠券模板校验、商品与分类属性、购物车、用户券分页、支付用券状态流转、结算可用券查询、优惠计算策略、分发策略选择以及框架基础响应能力。

当前仍有部分测试属于“本地工具 / 冒烟 / 集成验证”性质，不建议直接纳入默认 CI，例如真实 RocketMQ、Redis、Elasticsearch、造数和建表 SQL 生成类测试。

## 2. 模块测试资产统计

| 模块 | 当前测试文件数 | 新增文件数 | 当前主要覆盖 | 仍需补齐 |
| --- | ---: | ---: | --- | --- |
| `merchant-admin` | 16 | 5 | 用户注册登录、JWT、密码、优惠券模板参数校验、模板创建/终止/增发、商品、分类、属性 | CouponTask、Excel 导入、真实 DB/Redis 一致性、日志断言 |
| `engine` | 9 | 5 | 购物车、用户券分页缓存补全、支付用券状态流转、提醒 bitmap、库存 Lua 返回值解析 | 同步/异步领券完整链路、MQ 消费者、真实 Redis Lua、支付回调集成 |
| `settlement` | 3 | 3 | 优惠计算策略、可用券/不可用券分类、商品券不匹配兜底 | `applySelectedCoupon` 尚未实现；并行/同步一致性需继续补 |
| `distribution` | 3 | 3 | 分发策略选择、重复 mark 拦截、消息渠道枚举、库存返回值解析 | 批量发券 Consumer、Excel 解析、MQ 幂等与失败重试 |
| `gateway` | 1 | 1 | 白名单、缺失/非法 Token、JWT 校验、用户 Header 透传 | 路由转发、限流、真实网关集成 |
| `framework` | 2 | 2 | `Result`/`Results`、SpEL 静态表达式与字面量解析 | 幂等切面、MQ 幂等切面、全局异常处理器 |
| `search` | 1 | 0 | Elasticsearch 连接冒烟 | Canal/MQ 同步、索引写入/更新/删除、搜索查询 |

## 3. 本轮新增测试清单

| 模块 | 测试类 | 覆盖业务能力 |
| --- | --- | --- |
| `framework` | `SpELUtilTest` | 幂等 key 相关 SpEL 字面量与静态表达式解析 |
| `framework` | `ResultsTest` | 统一成功/失败响应对象与 `isSuccess` / `isFail` |
| `settlement` | `CouponCalculationStrategyTest` | 立减、满减达标/不达标、折扣券计算 |
| `settlement` | `CouponQueryServiceImplTest` | 用户无券、全店券/商品券可用性分类、优惠金额排序、商品券不匹配不可用 |
| `settlement` | `CouponApplyServiceImplTest` | 标记 `applySelectedCoupon` 当前未实现，保留待启用失败用例 |
| `engine` | `CartServiceImplTest` | 加购、同商品合并、数量边界、购物车汇总金额/税费/下架兜底 |
| `engine` | `UserCouponServiceImplTest` | 用户券分页与 Redis 模板缓存字段补全 |
| `engine` | `CouponPayServiceImplTest` | 支付用券锁、金额校验、结算单创建、用券锁定、核销失败、退款恢复 |
| `engine` | `CouponTemplateRemindUtilTest` | 提醒 bitmap 计算、超限校验、提醒时间/类型还原 |
| `engine` | `StockDecrementReturnCombinedUtilTest` | Redis Lua 复合返回值字段解析 |
| `merchant-admin` | `CouponTemplateCreateParamFilterTest` | 优惠券模板必填、目标/商品互斥、满减/折扣规则风控 |
| `merchant-admin` | `CouponTemplateServiceImplTest` | 平台/商家券权限、责任链执行、模板创建、终止、增发失败、详情映射 |
| `merchant-admin` | `GoodsCategoryServiceImplTest` | 分类权限、三级限制、同级重名、分类树、删除子分类拦截 |
| `merchant-admin` | `GoodsAttributeServiceImplTest` | 属性权限、单选属性候选值、创建、更新不存在、列表映射 |
| `merchant-admin` | `GoodsServiceImplTest` | 商品参数校验、创建默认下架、状态更新、库存调整、上架商品删除拦截 |
| `distribution` | `DistributionStrategyChooseTest` | 策略注册、选择、执行、重复 mark 拦截、未知 mark 异常 |
| `distribution` | `SendMessageMarkCovertEnumTest` | 消息渠道 type 到策略 mark 映射 |
| `distribution` | `StockDecrementReturnCombinedUtilTest` | 批量发券库存扣减复合返回值解析 |
| `gateway` | `TokenValidateFilterTest` | 白名单放行、缺失 Token 401、非法 Token 401、合法 Token Header 透传 |

## 4. 测试覆盖矩阵

| 业务能力 | 所属模块 | 当前覆盖状态 | 对应测试 | 优先级 |
| --- | --- | --- | --- | --- |
| 用户注册/登录/账号状态 | `merchant-admin` | 已有单测覆盖 | `UserServiceImplTest` | P0 |
| JWT 生成/解析/校验 | `merchant-admin` | 已有单测覆盖 | `JWTUtilTest`, `PasswordEncoderTest` | P0 |
| 网关白名单与 JWT 鉴权 | `gateway` | 新增单测覆盖 | `TokenValidateFilterTest` | P0 |
| 用户身份 Header 透传 | `gateway` | 新增单测覆盖 | `TokenValidateFilterTest` | P0 |
| 优惠券模板参数校验 | `merchant-admin` | 新增单测覆盖 | `CouponTemplateCreateParamFilterTest` | P0 |
| 优惠券模板创建/终止/增发 | `merchant-admin` | 新增单测覆盖主要分支 | `CouponTemplateServiceImplTest` | P0 |
| 商品创建/状态/库存/删除 | `merchant-admin` | 新增单测覆盖主要分支 | `GoodsServiceImplTest` | P0 |
| 商品分类树与权限 | `merchant-admin` | 新增单测覆盖 | `GoodsCategoryServiceImplTest` | P1 |
| 商品属性创建与查询 | `merchant-admin` | 新增单测覆盖 | `GoodsAttributeServiceImplTest` | P1 |
| 购物车加购与汇总 | `engine` | 新增单测覆盖 | `CartServiceImplTest` | P0 |
| 用户券分页查询 | `engine` | 新增单测覆盖 | `UserCouponServiceImplTest` | P1 |
| 支付用券/核销/退款状态流转 | `engine` | 新增单测覆盖主要分支 | `CouponPayServiceImplTest` | P0 |
| 提醒 bitmap 计算 | `engine` | 新增单测覆盖 | `CouponTemplateRemindUtilTest` | P1 |
| 优惠计算策略 | `settlement` | 新增单测覆盖 | `CouponCalculationStrategyTest` | P0 |
| 可用券查询与分类 | `settlement` | 新增单测覆盖同步版本 | `CouponQueryServiceImplTest` | P0 |
| 应用指定优惠券 | `settlement` | 未实现，已标记待启用测试 | `CouponApplyServiceImplTest` | P0 |
| 分发策略选择 | `distribution` | 新增单测覆盖 | `DistributionStrategyChooseTest` | P1 |
| 消息渠道映射 | `distribution` | 新增单测覆盖 | `SendMessageMarkCovertEnumTest` | P1 |
| 统一响应对象 | `framework` | 新增单测覆盖 | `ResultsTest` | P1 |
| SpEL key 解析 | `framework` | 新增基础单测覆盖 | `SpELUtilTest` | P1 |
| 幂等切面 | `framework` | 缺失 | 建议 `NoDuplicateSubmitAspectTest`, `NoMQDuplicateConsumeAspectTest` | P1 |
| 搜索索引同步 | `search` | 仅连接冒烟 | 建议 Consumer 与 Repository 测试 | P1 |
| 前端主流程 | `frontend` | 缺失 | 建议 Playwright E2E | P0 |

## 5. 本轮发现并修复的问题

| 问题 | 风险 | 修复 |
| --- | --- | --- |
| `CouponTemplateCreateParamBaseVerifyChainFilter` 使用 `Integer` 与枚举对象直接比较，导致全店券/商品专属券互斥规则失效 | 可创建错误优惠券规则，P0 | 改为与 `DiscountTargetEnum.*.getType()` 比较，并用测试覆盖 |
| `CouponTemplateCreateParamVerifyChainFilter` 商品专属券深层规则判断同样使用错误比较方式 | 满减/折扣券风控校验可能绕过，P0 | 改为比较 `getType()`，并用规则测试覆盖 |
| `CouponQueryServiceImpl.listQueryUserCouponsBySync` 商品券不匹配后加入不可用列表但继续解引用 `couponGoods` | 商品券不匹配时可能 NPE，P0 | 增加 `return`，并用商品券不匹配测试覆盖 |

## 6. 建议纳入 CI 的测试

建议优先纳入默认 CI：

- `framework`: `SpELUtilTest`, `ResultsTest`
- `gateway`: `TokenValidateFilterTest`
- `settlement`: `CouponCalculationStrategyTest`, `CouponQueryServiceImplTest`
- `engine`: `CartServiceImplTest`, `UserCouponServiceImplTest`, `CouponPayServiceImplTest`, `CouponTemplateRemindUtilTest`, `StockDecrementReturnCombinedUtilTest`
- `merchant-admin`: `UserServiceImplTest`, `JWTUtilTest`, `PasswordEncoderTest`, `CouponTemplateCreateParamFilterTest`, `CouponTemplateServiceImplTest`, `GoodsServiceImplTest`, `GoodsCategoryServiceImplTest`, `GoodsAttributeServiceImplTest`
- `distribution`: `DistributionStrategyChooseTest`, `SendMessageMarkCovertEnumTest`, `StockDecrementReturnCombinedUtilTest`

建议隔离到集成测试 Profile 或手工执行：

- RocketMQ 相关测试
- Redis/Elasticsearch 真实连接测试
- 造数、Excel 生成、建表 SQL 生成类测试
- `engine` 中带无限等待风险的 MQ 延迟消息测试

