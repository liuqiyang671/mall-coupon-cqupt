# 后端单元测试补齐报告

> 更新日期：2026-05-09  
> 范围：`framework`、`gateway`、`merchant-admin`、`engine`、`settlement`、`distribution` 后端模块  
> 目标：围绕 P0 / P1 业务逻辑补齐可稳定运行的单元测试，并输出测试资产文档

## 1. 本轮完成内容

本轮新增 19 个后端测试类，当前后端测试相关 Java 文件总数为 35 个。新增测试不依赖真实 MySQL、Redis、RocketMQ、Elasticsearch，主要通过 JUnit 5、Mockito、Spring Mock WebFlux 覆盖业务规则和关键分支。

新增测试覆盖：

- 网关鉴权：白名单、缺失 Token、非法 Token、合法 JWT Header 透传。
- 商家端：优惠券模板参数校验、模板创建/终止/增发、商品创建/状态/库存/删除、分类树、属性管理。
- 引擎端：购物车加购/汇总、用户券分页缓存补全、支付用券创建结算单/锁券/核销/退款、提醒 bitmap。
- 结算端：立减/满减/折扣策略、可用券/不可用券分类、商品券不匹配兜底。
- 分发端：策略注册/选择/重复 mark、消息渠道枚举、库存扣减结果解析。
- 框架层：统一响应对象、SpEL 基础解析。

## 2. 新增测试类

| 模块 | 新增测试类数量 | 新增测试类 |
| --- | ---: | --- |
| `framework` | 2 | `SpELUtilTest`, `ResultsTest` |
| `gateway` | 1 | `TokenValidateFilterTest` |
| `settlement` | 3 | `CouponCalculationStrategyTest`, `CouponQueryServiceImplTest`, `CouponApplyServiceImplTest` |
| `engine` | 5 | `CartServiceImplTest`, `UserCouponServiceImplTest`, `CouponPayServiceImplTest`, `CouponTemplateRemindUtilTest`, `StockDecrementReturnCombinedUtilTest` |
| `merchant-admin` | 5 | `CouponTemplateCreateParamFilterTest`, `CouponTemplateServiceImplTest`, `GoodsServiceImplTest`, `GoodsCategoryServiceImplTest`, `GoodsAttributeServiceImplTest` |
| `distribution` | 3 | `DistributionStrategyChooseTest`, `SendMessageMarkCovertEnumTest`, `StockDecrementReturnCombinedUtilTest` |

## 3. 顺手修复的问题

| 文件 | 修复说明 | 测试保护 |
| --- | --- | --- |
| `CouponTemplateCreateParamBaseVerifyChainFilter.java` | 将 `Integer` 与枚举对象比较改为与 `getType()` 比较，修复全店券/商品专属券互斥校验失效 | `CouponTemplateCreateParamFilterTest` |
| `CouponTemplateCreateParamVerifyChainFilter.java` | 修复商品专属券深层规则校验条件，使满减/折扣风控规则能执行 | `CouponTemplateCreateParamFilterTest` |
| `CouponQueryServiceImpl.java` | 商品券不匹配时加入不可用列表后立即返回，避免后续解引用 `couponGoods` 触发 NPE | `CouponQueryServiceImplTest` |

## 4. 验证结果

已执行总验证命令：

```bash
mvn -pl framework,settlement,engine,merchant-admin,distribution,gateway -am "-Dtest=SpELUtilTest,ResultsTest,CouponCalculationStrategyTest,CouponQueryServiceImplTest,CouponApplyServiceImplTest,StockDecrementReturnCombinedUtilTest,CouponTemplateRemindUtilTest,CartServiceImplTest,UserCouponServiceImplTest,CouponPayServiceImplTest,CouponTemplateCreateParamFilterTest,CouponTemplateServiceImplTest,GoodsCategoryServiceImplTest,GoodsAttributeServiceImplTest,GoodsServiceImplTest,SendMessageMarkCovertEnumTest,DistributionStrategyChooseTest,TokenValidateFilterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：

- 执行测试：68
- 失败：0
- 错误：0
- 跳过：1
- 跳过原因：`CouponApplyServiceImpl.applySelectedCoupon` 当前未实现，保留待启用测试。

构建过程中存在 Maven 警告：`framework/pom.xml` 中 `hutool-all` 依赖重复声明。该警告未影响本轮测试结果，但建议后续清理。

## 5. 未覆盖与原因

| 模块 | 未覆盖项 | 原因 | 建议 |
| --- | --- | --- | --- |
| `settlement` | `applySelectedCoupon` 成功/失败逻辑 | Service 当前直接返回 `null` | 先实现业务逻辑，再启用 `CouponApplyServiceImplTest` |
| `engine` | 真实领券 Redis Lua + DB 落库 | 需要 Redis、事务、分库表环境 | 建设 Testcontainers 或集成测试 Profile |
| `engine` / `distribution` | MQ Producer/Consumer 完整链路 | 需要 RocketMQ 环境，现有测试有阻塞风险 | 将 MQ 测试隔离到 integration profile |
| `search` | Elasticsearch 索引同步与查询 | 需要 ES 或 Testcontainers | 新增 Consumer 单测和 ES 集成测试 |
| `framework` | 幂等切面、全局异常处理 | 本轮先覆盖基础工具与响应对象 | 后续补切面级测试 |
| `frontend` | 登录到结算 E2E | 本轮聚焦后端单元测试 | 建议 Playwright |

## 6. 建议下一步

1. 实现 `CouponApplyServiceImpl.applySelectedCoupon`，启用当前 `@Disabled` 用例，并补充无效券、过期券、金额精度、商品券不匹配测试。
2. 将外部依赖测试按 `unit` / `integration` Profile 隔离，避免默认测试被真实 Redis/MQ/ES 阻塞。
3. 增加 JaCoCo 覆盖率统计，先建立模块基线，再逐步提高 P0 模块门槛。
4. 为 `framework` 幂等切面补测试，覆盖重复提交、异常释放、SpEL key、MQ 重复消费。
5. 为前端补 Playwright 主链路：登录、领券、购物车、结算、选择优惠券。

