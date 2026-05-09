# 后端单元测试补齐报告

> 更新日期：2026-05-09  
> 范围：`framework`、`gateway`、`merchant-admin`、`engine`、`settlement`、`distribution` 后端模块。  
> 目标：围绕 P0 / P1 业务逻辑补齐可稳定运行的单元测试，并记录测试资产和剩余缺口。

## 1. 当前完成内容

本轮已完成 `settlement` 模块 P0 缺口：`CouponApplyServiceImpl.applySelectedCoupon` 不再返回 `null`，已具备指定优惠券应用能力。

新增和启用的测试覆盖：

- 指定券可用时返回订单原金额、优惠后金额和已应用券 ID。
- 券不存在或不属于当前用户时抛出 `ClientException`。
- 用户券过期、模板不可用时拒绝应用。
- 商品券与订单商品不匹配时拒绝应用。
- 商品券按匹配商品金额计算优惠。
- 优惠金额大于订单金额时，最终应付金额兜底为 `0`。

所有测试均使用 Mockito 构造 `UserCouponMapper`、`CouponTemplateMapper` 和 `CouponCalculationService`，不连接真实 Redis / MySQL。

## 2. 测试资产

| 模块 | 测试类数量 | 测试类 |
| --- | ---: | --- |
| `framework` | 2 | `SpELUtilTest`, `ResultsTest` |
| `gateway` | 1 | `TokenValidateFilterTest` |
| `settlement` | 3 | `CouponCalculationStrategyTest`, `CouponQueryServiceImplTest`, `CouponApplyServiceImplTest` |
| `engine` | 5 | `CartServiceImplTest`, `UserCouponServiceImplTest`, `CouponPayServiceImplTest`, `CouponTemplateRemindUtilTest`, `StockDecrementReturnCombinedUtilTest` |
| `merchant-admin` | 5 | `CouponTemplateCreateParamFilterTest`, `CouponTemplateServiceImplTest`, `GoodsServiceImplTest`, `GoodsCategoryServiceImplTest`, `GoodsAttributeServiceImplTest` |
| `distribution` | 3 | `DistributionStrategyChooseTest`, `SendMessageMarkCovertEnumTest`, `StockDecrementReturnCombinedUtilTest` |

## 3. 已修复问题

| 文件 | 修复说明 | 测试保护 |
| --- | --- | --- |
| `CouponTemplateCreateParamBaseVerifyChainFilter.java` | 将 `Integer` 与枚举对象比较改为与 `getType()` 比较，修复全店券 / 商品专属券互斥校验失效。 | `CouponTemplateCreateParamFilterTest` |
| `CouponTemplateCreateParamVerifyChainFilter.java` | 修复商品专属券深层规则校验条件，使满减 / 折扣风控规则能执行。 | `CouponTemplateCreateParamFilterTest` |
| `CouponQueryServiceImpl.java` | 商品券不匹配时加入不可用列表后立即返回，避免后续解引用 `couponGoods` 触发 NPE。 | `CouponQueryServiceImplTest` |
| `CouponApplyServiceImpl.java` | 实现指定券应用逻辑，补齐用户券归属、状态、有效期、商品匹配、优惠计算和最终金额兜底。 | `CouponApplyServiceImplTest` |

## 4. 验证结果

### Settlement 回归

```bash
mvn -pl settlement -am test
```

结果：

- 执行测试：18
- 失败：0
- 错误：0
- 跳过：0
- 其中 `settlement` 模块执行 13 个测试，全部通过。

### P0 红绿验证

红灯验证命令：

```bash
mvn -pl settlement -am "-Dtest=CouponApplyServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

红灯结果：`CouponApplyServiceImplTest` 7 个用例全部失败，失败原因是 `applySelectedCoupon` 空实现返回 `null` 或未抛出业务异常。

绿灯结果：实现后同一命令通过，`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`。

构建过程仍存在既有 Maven 警告：`framework/pom.xml` 中 `hutool-all` 依赖重复声明。该警告未影响本轮测试结果，建议后续清理。

## 5. 剩余缺口

| 模块 | 未覆盖项 | 原因 | 建议 |
| --- | --- | --- | --- |
| `engine` | 真实领券 Redis Lua + DB 落库 | 需要 Redis、事务、分库分表环境。 | 建设 Testcontainers 或集成测试 Profile。 |
| `engine` / `distribution` | MQ Producer / Consumer 完整链路 | 需要 RocketMQ 环境。 | 保持 `*IT` 隔离，使用专用 integration 命令运行。 |
| `search` | Elasticsearch 索引同步与查询 | 需要 ES 或 Testcontainers。 | 新增 Consumer 单测和 ES 集成测试。 |
| `framework` | 幂等切面、全局异常处理 | 当前仅覆盖基础工具和响应对象。 | 补切面级测试，覆盖重复提交、异常释放、SpEL key。 |
| `frontend` | 登录到结算 E2E | 本报告聚焦后端单元测试。 | 使用 Playwright 覆盖登录、领券、购物车、结算、选择优惠券。 |

## 6. 建议下一步

1. 补充 `CouponApplyController` Web 层测试，验证 `/api/settlement/apply-coupon/{couponId}` 的参数传递和异常响应。
2. 为 `applySelectedCoupon` 增加折扣券、满减券真实策略计算的端到端单元测试，减少对 mock 返回优惠金额的依赖。
3. 将外部依赖测试继续按 `unit` / `integration` 隔离执行，避免默认测试访问真实 Redis / RocketMQ / Elasticsearch / MySQL。
4. 增加 JaCoCo 覆盖率统计，先建立模块基线，再逐步提高 P0 模块门槛。
