# 测试执行指南

## 测试分层

本项目按执行稳定性和外部依赖将测试分为两类：

| 类型 | 命名约定 | JUnit 标记 | 默认 `mvn test` | 适用范围 |
| --- | --- | --- | --- | --- |
| unit | `*Test.java` / `*Tests.java` | 无强制要求 | 执行 | 不依赖真实 Redis、RocketMQ、Elasticsearch、MySQL 的快速单元测试 |
| integration | `*IT.java` | `@Tag("integration")` | 不执行 | 需要 Spring 容器、真实中间件、真实数据库、文件生成或批量造数的测试 |

`*IT.java` 不在 Maven Surefire 默认包含规则内，因此常规单元测试不会误触发外部服务，也不会执行耗时造数或阻塞等待。

## 默认单元测试

执行目标模块的默认单元测试：

```bash
mvn -pl engine,merchant-admin,search -am test
```

执行全项目默认单元测试：

```bash
mvn test
```

默认命令只应运行稳定的 unit 测试。若新增测试依赖真实 Redis、RocketMQ、Elasticsearch、MySQL、外部文件系统或完整 Spring 容器，应命名为 `*IT.java` 并添加 `@Tag("integration")`。

## 集成测试

按模块执行全部 integration 测试：

```bash
mvn -pl engine,merchant-admin,search -Dtest=*IT test
```

只执行某一个集成测试：

```bash
mvn -pl merchant-admin -Dtest=RocketMQ5xProducerConsumerIT test
```

只执行带 `integration` 标签的集成测试：

```bash
mvn -pl engine,merchant-admin,search -Dtest=*IT -Dgroups=integration test
```

## 外部依赖准备

执行 integration 测试前，需要确认对应服务可用：

| 依赖 | 涉及测试 | 说明 |
| --- | --- | --- |
| Redis | `engine/.../CouponTemplateServiceRemindIT.java` | 使用 `TEST_REDIS_HOST`、`TEST_REDIS_PORT`、`TEST_REDIS_PASSWORD` 覆盖连接信息 |
| RocketMQ | `engine/.../RocketMQ5xDelayProducerConsumerIT.java`、`merchant-admin/.../mq/*IT.java` | 消费等待已使用 `CountDownLatch` 超时控制，不允许无限等待 |
| Elasticsearch | `search/.../InitElasticsearchIT.java` | 需要可访问的 Elasticsearch 集群和项目配置 |
| MySQL | `merchant-admin/.../MerchantAdminApplicationIT.java`、`CouponTemplateIT.java`、`Mock*DataIT.java` | 会写入真实数据库，建议使用专用测试库 |
| 文件系统 | `merchant-admin/.../task/ExcelGenerateIT.java` | 会向项目上级目录的 `tmp` 目录写入 Excel 文件 |

Redis 环境变量示例：

```bash
TEST_REDIS_HOST=127.0.0.1 TEST_REDIS_PORT=6379 TEST_REDIS_PASSWORD= mvn -pl engine -Dtest=CouponTemplateServiceRemindIT test
```

PowerShell 示例：

```powershell
$env:TEST_REDIS_HOST="127.0.0.1"
$env:TEST_REDIS_PORT="6379"
$env:TEST_REDIS_PASSWORD=""
mvn -pl engine -Dtest=CouponTemplateServiceRemindIT test
```

## 新增测试约束

新增或修改测试时遵守以下规则：

1. 依赖真实 Redis、RocketMQ、Elasticsearch、MySQL 的测试必须使用 `*IT.java` 命名，并添加 `@Tag("integration")`。
2. 默认 `mvn test` 不应访问外部中间件、真实数据库或公网地址。
3. 阻塞式等待必须有明确超时，禁止使用 `while (true)`。
4. `Thread.sleep` 只能用于有边界的造数节流或明确的集成测试场景；消息消费等待优先使用 `CountDownLatch`、Awaitility 或类似超时机制。
5. 测试代码禁止写入真实公网 IP、真实密码和个人密钥；连接信息使用环境变量或测试配置覆盖。
