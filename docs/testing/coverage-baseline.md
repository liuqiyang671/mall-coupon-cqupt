# JaCoCo 覆盖率基线

## 配置范围

本次在父 `pom.xml` 统一引入 JaCoCo，所有 Maven 子模块继承同一套覆盖率配置：

- JaCoCo 版本：`0.8.12`
- Surefire 版本：`3.2.5`
- 默认阶段：`mvn test`
- 报告目标：每个有测试执行数据的模块生成 `target/site/jacoco/index.html` 和 `target/site/jacoco/jacoco.csv`

当前不设置强制覆盖率阈值，先建立可重复执行的基线。待核心链路单测稳定后，再按模块逐步增加 `jacoco:check` 阈值。

## 测试隔离策略

覆盖率统计沿用 T01 的测试隔离策略：

- 默认 `mvn test` 只执行稳定 unit 测试。
- 依赖真实 Redis、RocketMQ、Elasticsearch、MySQL、文件系统造数或完整 Spring 容器的测试，继续使用 `*IT.java` 命名并添加 `@Tag("integration")`。
- 父 POM 的 Surefire 配置默认排除 `integration` 标签、`**/*IT.java` 和 `**/*ITCase.java`，避免外部依赖测试进入默认覆盖率统计。
- 不删除 integration 测试；需要验证外部依赖链路时，仍按 `docs/testing/test-execution-guide.md` 中的 integration 命令执行。

## 执行命令

生成全工程默认单元测试覆盖率：

```bash
mvn test
```

按模块生成覆盖率：

```bash
mvn -pl settlement -am test
mvn -pl engine -am test
mvn -pl merchant-admin -am test
```

如果只验证 `search` 新增的本地单测：

```bash
mvn -pl search -am "-Dtest=MessageWrapperTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

## 报告路径

| 模块 | HTML 报告 | CSV 数据 |
| --- | --- | --- |
| `framework` | `framework/target/site/jacoco/index.html` | `framework/target/site/jacoco/jacoco.csv` |
| `distribution` | `distribution/target/site/jacoco/index.html` | `distribution/target/site/jacoco/jacoco.csv` |
| `search` | `search/target/site/jacoco/index.html` | `search/target/site/jacoco/jacoco.csv` |
| `settlement` | `settlement/target/site/jacoco/index.html` | `settlement/target/site/jacoco/jacoco.csv` |
| `merchant-admin` | `merchant-admin/target/site/jacoco/index.html` | `merchant-admin/target/site/jacoco/jacoco.csv` |
| `engine` | `engine/target/site/jacoco/index.html` | `engine/target/site/jacoco/jacoco.csv` |
| `gateway` | `gateway/target/site/jacoco/index.html` | `gateway/target/site/jacoco/jacoco.csv` |

父聚合工程 `mall-cqupt-lqy11` 是 `pom` 包装类型，不包含业务 class，JaCoCo 会跳过父工程自身报告。

## 本次验证结果

执行时间：2026-05-09 22:26:17 +08:00

命令：

```bash
mvn test
```

结果：

- Reactor：`BUILD SUCCESS`
- 默认单测总数：110
- Failures：0
- Errors：0
- Skipped：0
- 未执行 `*IT` 集成测试，未连接真实 Redis、RocketMQ、Elasticsearch、MySQL。

| 模块 | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| `framework` | 5 | 0 | 0 | 0 |
| `distribution` | 7 | 0 | 0 | 0 |
| `search` | 3 | 0 | 0 | 0 |
| `settlement` | 13 | 0 | 0 | 0 |
| `merchant-admin` | 63 | 0 | 0 | 0 |
| `engine` | 15 | 0 | 0 | 0 |
| `gateway` | 4 | 0 | 0 | 0 |

## 模块覆盖率基线

统计口径：JaCoCo `jacoco.csv` 汇总值，按模块统计 instruction、branch、line、method 覆盖率。

| 模块 | 指令覆盖率 | 分支覆盖率 | 行覆盖率 | 方法覆盖率 |
| --- | ---: | ---: | ---: | ---: |
| `framework` | 34.01% | 25.00% | 29.34% | 21.15% |
| `distribution` | 2.81% | 1.14% | 6.07% | 3.09% |
| `search` | 5.52% | 0.42% | 16.95% | 11.76% |
| `settlement` | 19.31% | 6.26% | 38.58% | 29.84% |
| `merchant-admin` | 19.87% | 6.33% | 42.36% | 33.95% |
| `engine` | 13.93% | 3.31% | 28.36% | 25.80% |
| `gateway` | 68.48% | 62.50% | 61.40% | 38.46% |

## 后续提升建议

1. 优先提升 P0 业务链路覆盖率：`settlement` 的优惠券试算/指定券应用、`engine` 的领券/锁券/核销、`merchant-admin` 的券模板创建与审核。
2. 对 `distribution` 补齐策略选择、批次分发、库存扣减、消息构造的纯单元测试；该模块当前覆盖率最低。
3. 对 `search` 先补本地映射、消息事件解析和文档转换测试；真实 Elasticsearch 初始化继续保留为 integration 测试。
4. 对 `merchant-admin` 和 `engine` 的 MyBatis 查询条件、异常分支、幂等与事务失败路径补单测，避免只覆盖 happy path。
5. 覆盖率稳定后分阶段开启阈值：先只设增量门禁，再设置模块级 `line` 或 `instruction` 最低阈值，避免一次性卡死 CI。
6. CI 中建议保留两条流水线：默认 `mvn test` 生成单测覆盖率，手动或定时任务运行 integration 测试。
