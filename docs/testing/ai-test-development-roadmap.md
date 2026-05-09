# 测试开发 AI 执行路线图

> 项目：OneCoupon 邮惠券商城  
> 日期：2026-05-09  
> 目标读者：准备转型测试开发工程师的项目负责人，以及后续接手执行的 AI Agent  
> 关联文档：[`test-asset-map.md`](./test-asset-map.md)、[`risk-matrix.md`](./risk-matrix.md)、[`backend-unit-test-completion-report.md`](./backend-unit-test-completion-report.md)

## 1. 当前项目测开现状

OneCoupon 是一个前后端分离的优惠券商城项目。后端是 Spring Cloud 多模块工程，包含 `merchant-admin`、`engine`、`settlement`、`distribution`、`search`、`gateway` 和 `framework`；前端是 Vue 3 + TypeScript + Vite 应用。

这个项目适合作为测试开发作品集，原因是它不只是 CRUD 系统，而是覆盖了登录鉴权、商品管理、优惠券模板、领券、购物车、结算、批量发券、预约提醒、RocketMQ、Redis、Elasticsearch、分库分表和网关鉴权等复杂链路。后续测试开发工作可以体现测试设计、自动化开发、测试数据治理、质量度量和缺陷闭环能力。

### 1.1 已有测试资产

当前后端测试类统计如下：

| 模块 | 测试类数量 | 现状判断 |
| --- | ---: | --- |
| `merchant-admin` | 16 | 已覆盖用户、JWT、券模板、商品、分类、属性等核心 Service 单元测试，但 Controller/API、任务链路和真实 DB/Redis 一致性仍不足。 |
| `engine` | 9 | 已覆盖购物车、用户券、支付用券、提醒工具和库存 Lua 返回值解析，但真实领券、MQ 消费和 Redis Lua 集成仍不足。 |
| `settlement` | 3 | 已覆盖优惠计算策略和可用券查询；`applySelectedCoupon` 仍未实现，属于 P0 阻塞点。 |
| `distribution` | 3 | 已覆盖分发策略选择、消息渠道映射和库存返回值解析；批量发券 Consumer、Excel 解析和失败记录仍不足。 |
| `framework` | 2 | 已覆盖统一响应和 SpEL 解析；幂等切面和全局异常处理仍不足。 |
| `gateway` | 1 | 已覆盖白名单、缺失 Token、非法 Token 和用户 Header 透传。 |
| `search` | 1 | 只有 Elasticsearch 冒烟测试，搜索同步链路基本缺失。 |

### 1.2 主要风险点

| 风险 | 影响 | 优先级 |
| --- | --- | --- |
| `settlement` 的 `CouponApplyServiceImpl.applySelectedCoupon` 仍返回 `null` | 结算闭环无法真正应用指定优惠券，直接影响交易主链路。 | P0 |
| 默认测试集中混入真实 Redis、RocketMQ、Elasticsearch、阻塞循环测试 | `mvn test` 不适合直接纳入 CI，容易卡死或依赖外部服务失败。 | P0 |
| 项目配置和测试中存在真实外部地址、密码等敏感信息 | 存在泄露风险，也会导致测试环境不可复现。 | P0 |
| 前端没有 Vitest、Playwright、coverage 脚本 | 只能人工验证页面，无法形成前端自动化回归。 | P0 |
| 缺少 JaCoCo 覆盖率基线和 CI 质量门禁 | 测试改进无法量化，面试展示说服力不足。 | P1 |
| API 自动化、测试数据治理、性能压测尚未工程化 | 无法支撑端到端回归、压测和质量报告。 | P1 |

## 2. AI 执行总原则

后续让 AI 做事时，不建议一次性发“把测试全部补齐”。这个项目模块多、外部依赖多，最好按任务卡逐个执行，并限制每次修改范围。

### 2.1 通用约束

每次给 AI 的提示词都建议包含以下约束：

- 先阅读 `docs/testing/test-asset-map.md`、`docs/testing/risk-matrix.md` 和当前任务相关源码。
- 不连接真实外部 IP、真实 Redis、真实 RocketMQ、真实 Elasticsearch。
- 不提交真实密码、Token、手机号等敏感信息；发现已有敏感信息时，优先改为环境变量或测试占位值。
- 不重构无关业务代码，不删除用户已有改动。
- 单元测试优先使用 JUnit 5 + Mockito；集成测试必须隔离到独立 profile、Tag 或 `*IT` 命名。
- 每个任务必须输出验证命令、执行结果、修改文件和剩余风险。
- 涉及前端测试时，必须同步更新 `frontend/package.json` 脚本。

### 2.2 通用 AI 提示词模板

```text
你是本项目的测试开发工程师。请在当前仓库中完成一个独立测试开发任务。

开始前请先阅读：
1. docs/testing/test-asset-map.md
2. docs/testing/risk-matrix.md
3. docs/testing/backend-unit-test-completion-report.md
4. 本任务涉及的源码和测试文件

执行要求：
- 只修改本任务必要文件，不做无关重构。
- 不连接真实外部 Redis、RocketMQ、Elasticsearch、MySQL，不使用真实 IP 和密码。
- 单元测试优先使用 JUnit 5 + Mockito。
- 集成测试必须隔离到独立 profile、JUnit Tag 或 *IT 命名，不能污染默认 mvn test。
- 代码修改后运行可执行的验证命令。
- 最后输出：修改文件、验证命令、测试结果、发现的问题、剩余风险。
```

## 3. 推荐执行顺序

### 3.1 第一阶段：让测试能稳定跑起来

目标：把当前测试从“能写”推进到“能稳定执行、可进入 CI”。

| 顺序 | 任务 | 价值 |
| ---: | --- | --- |
| 1 | 隔离不稳定测试和外部依赖测试 | 先保证默认测试不会卡死、不会连真实服务。 |
| 2 | 修复敏感配置和测试外部地址 | 提高项目安全性和可复现性。 |
| 3 | 实现 `applySelectedCoupon` 并启用回归测试 | 打通结算 P0 链路。 |
| 4 | 引入 JaCoCo 覆盖率基线 | 让测试成果可量化。 |

### 3.2 第二阶段：补齐自动化测试体系

目标：从后端单测扩展到 API、前端和集成测试。

| 顺序 | 任务 | 价值 |
| ---: | --- | --- |
| 5 | 建设 API 自动化集合 | 覆盖登录、建券、领券、购物车、结算主链路。 |
| 6 | 建设前端 Vitest 单元测试 | 覆盖工具函数、Store、API 封装。 |
| 7 | 建设 Playwright E2E | 覆盖真实用户操作路径。 |
| 8 | 建设测试数据初始化和清理脚本 | 让 API/E2E/性能测试可重复执行。 |

### 3.3 第三阶段：做出测开作品集亮点

目标：形成能在简历和面试中讲清楚的质量工程方案。

| 顺序 | 任务 | 价值 |
| ---: | --- | --- |
| 9 | 建设 Testcontainers 集成测试 | 覆盖 Redis Lua、DB 分片、ES 同步等复杂依赖。 |
| 10 | 建设性能压测方案 | 验证领券、购物车、结算等高频链路。 |
| 11 | 建设安全测试和权限测试 | 覆盖网关、角色权限、越权访问。 |
| 12 | 建设 CI 和质量报告 | 形成持续反馈闭环。 |

## 4. AI 可执行任务卡

### T01：隔离不稳定测试和外部依赖测试

**优先级：P0**

当前 `engine`、`merchant-admin`、`search` 中存在 `@SpringBootTest`、真实 Redis、真实 RocketMQ、Elasticsearch、`while (true)`、`Thread.sleep` 等测试。这类测试不能进入默认 CI。

**建议交付物：**

- 给外部依赖测试添加 `@Tag("integration")` 或改名为 `*IT`。
- 将阻塞式测试改为有超时、有断言的集成测试，或默认禁用。
- 在文档中标明默认单测命令和集成测试命令。
- 更新 `docs/testing/test-asset-map.md` 或新增 `docs/testing/test-execution-guide.md`。

**验收标准：**

- 默认单元测试不会连接真实 Redis、MQ、ES。
- 默认单元测试不会无限等待。
- 文档明确说明哪些测试适合 CI，哪些需要本地集成环境。

**AI 提示词：**

```text
请完成“隔离不稳定测试和外部依赖测试”任务。

重点检查：
- engine/src/test
- merchant-admin/src/test
- search/src/test
- 所有 @SpringBootTest、RocketMQTemplate、ElasticsearchTemplate、Thread.sleep、while (true)、真实 IP、真实密码

要求：
1. 将依赖真实 Redis、RocketMQ、Elasticsearch、MySQL 的测试隔离为 integration 测试，默认 mvn test 不应执行它们。
2. 对阻塞式测试增加超时控制，不能保留 while (true) 这类无限等待。
3. 不删除有价值的测试逻辑，只做隔离、标记或改造。
4. 新增或更新 docs/testing/test-execution-guide.md，说明 unit、integration 的执行方式。
5. 运行默认单元测试命令，证明不会卡死。

输出修改文件、验证命令、测试结果和剩余风险。
```

### T02：修复测试配置中的敏感信息和外部地址

**优先级：P0**

项目中存在硬编码的外部 IP、Redis 密码、数据库密码等配置。测开作品集必须体现安全意识和环境可复现能力。

**建议交付物：**

- 将 `application.yaml`、`shardingsphere-config.yaml`、测试注解中的真实地址和密码改为环境变量占位。
- 提供 `application-local.example.yaml` 或文档说明。
- 补充一份安全配置检查清单。

**验收标准：**

- `rg "43\\.139|Lqy259931|password:"` 的结果只允许出现在示例、文档说明或环境变量占位场景。
- 项目仍能在本地通过环境变量配置运行。

**AI 提示词：**

```text
请完成“敏感信息和外部地址清理”任务。

要求：
1. 扫描全仓库中的真实 IP、真实密码、硬编码 Redis/MySQL/ES 连接信息。
2. 将运行配置改为环境变量占位，例如 ${REDIS_HOST:127.0.0.1}、${REDIS_PASSWORD:}。
3. 不改变业务逻辑。
4. 为本地开发补充示例配置或 docs/testing/env-config-guide.md。
5. 运行 rg 命令验证敏感信息是否仍存在，并说明保留项原因。

注意：不要输出真实密码，不要把真实外部地址写进新文档。
```

### T03：实现结算指定优惠券应用链路

**优先级：P0**

`settlement/src/main/java/com/cqupt/settlement/service/impl/CouponApplyServiceImpl.java` 当前直接返回 `null`。这是交易主链路的明显缺口，适合作为“缺陷发现 -> 单测复现 -> 修复 -> 回归”的测开案例。

**建议交付物：**

- 实现 `applySelectedCoupon`。
- 启用并扩展 `CouponApplyServiceImplTest`。
- 覆盖成功应用、无效券、不可用券、金额精度、商品券不匹配等场景。
- 更新 `docs/testing/backend-unit-test-completion-report.md`。

**验收标准：**

- `CouponApplyServiceImplTest` 不再 `@Disabled`。
- 成功和失败分支都有断言。
- 不引入真实 Redis/MySQL 依赖。

**AI 提示词：**

```text
请完成 settlement 模块的 P0 缺口：实现 CouponApplyServiceImpl.applySelectedCoupon 并补齐单元测试。

开始前请阅读：
- settlement/src/main/java/com/cqupt/settlement/service/CouponApplyService.java
- settlement/src/main/java/com/cqupt/settlement/service/impl/CouponApplyServiceImpl.java
- settlement/src/main/java/com/cqupt/settlement/service/impl/CouponQueryServiceImpl.java
- settlement/src/main/java/com/cqupt/settlement/service/strategy/*
- settlement/src/main/java/com/cqupt/settlement/dto/req/*
- settlement/src/main/java/com/cqupt/settlement/dto/resp/*
- settlement/src/test/java/com/cqupt/settlement/service/impl/CouponApplyServiceImplTest.java

要求：
1. 基于现有 DTO、Service 和策略实现，不新建无必要的大抽象。
2. 先补失败用例，再实现逻辑，再启用回归测试。
3. 测试覆盖：指定券可用、券不存在或不属于用户、券过期或不可用、商品券不匹配、优惠金额计算、最终应付金额不能为负。
4. 不连接真实 Redis/MySQL，使用 Mockito 构造依赖。
5. 更新 docs/testing/backend-unit-test-completion-report.md 中的缺口状态。
6. 运行 settlement 相关测试并输出结果。
```

### T04：引入 JaCoCo 覆盖率基线

**优先级：P1**

当前项目没有统一覆盖率报告。引入 JaCoCo 后，可以把“补了很多测试”转成可量化成果。

**建议交付物：**

- 父 POM 或模块 POM 中引入 JaCoCo。
- 生成 HTML/XML 覆盖率报告。
- 先建立基线，不要一开始设置过高门禁。
- 输出 `docs/testing/coverage-baseline.md`。

**验收标准：**

- 可以运行命令生成覆盖率报告。
- 文档记录每个模块的初始覆盖率。
- 默认门禁不会因为历史缺口导致全项目无法构建。

**AI 提示词：**

```text
请为后端 Maven 多模块工程引入 JaCoCo 覆盖率基线。

要求：
1. 优先在父 pom.xml 中统一配置，遵循现有 Maven 多模块结构。
2. 生成每个模块的覆盖率报告，必要时先不设置强制阈值。
3. 不让外部依赖测试进入默认覆盖率统计。
4. 新增 docs/testing/coverage-baseline.md，记录执行命令、报告路径、模块覆盖率和后续提升建议。
5. 运行覆盖率命令，输出结果。

注意：如果发现默认 mvn test 会执行不稳定测试，请先引用 T01 的隔离策略，不要为了通过覆盖率而删除测试。
```

### T05：建设后端 API 自动化回归集合

**优先级：P0**

后端核心接口已经比较完整，但缺少一套可重复执行的 API 自动化集合。

**建议优先覆盖链路：**

1. 用户注册、登录、获取用户信息。
2. 商家创建商品。
3. 商家创建优惠券模板。
4. 用户查询领券中心并领券。
5. 用户加入购物车、修改数量、选中商品。
6. 用户查询可用券。
7. 用户结算并应用优惠券。
8. 非法 Token、缺失 Token、越权访问。

**建议交付物：**

```text
tests/api/onecoupon.postman_collection.json
tests/api/local.postman_environment.json
tests/api/README.md
docs/testing/api-test-guide.md
```

**验收标准：**

- API 集合能通过环境变量串联 Token、用户 ID、商品 ID、券模板 ID、用户券 ID。
- 每个接口至少有状态码、业务 code、关键字段断言。
- 失败场景不能只测成功响应。

**AI 提示词：**

```text
请为项目建设 Postman/Newman API 自动化回归集合。

请先阅读：
- merchant-admin/src/main/java/**/controller
- engine/src/main/java/**/controller
- settlement/src/main/java/**/controller
- frontend/src/api
- resources/database/frontend-test-data.sql

要求：
1. 新建 tests/api/onecoupon.postman_collection.json 和 tests/api/local.postman_environment.json。
2. 覆盖登录、用户信息、商品管理、券模板、领券、购物车、查询可用券、应用优惠券、鉴权失败。
3. 使用环境变量串联动态数据，不要硬编码一次性 ID。
4. 为每个请求添加基本断言：HTTP 状态、业务 code、关键 data 字段。
5. 新增 docs/testing/api-test-guide.md，说明环境准备、执行命令和常见失败原因。
6. 如果当前后端无法完整启动，请先保证集合结构和断言脚本正确，并明确阻塞项。
```

### T06：建设前端 Vitest 单元测试

**优先级：P0**

前端当前只有 `dev`、`build`、`preview`、`typecheck`，没有 `test` 和 `coverage`。可以先从低成本、高稳定性的工具函数和 Store 测试切入。

**建议交付物：**

```text
frontend/src/utils/validators.test.ts
frontend/src/utils/coupon.test.ts
frontend/src/stores/auth.test.ts
frontend/src/stores/cart.test.ts
frontend/vitest.config.ts
```

**验收标准：**

- `frontend/package.json` 增加 `test:unit`、`coverage` 脚本。
- 能运行 `npm run test:unit`。
- 覆盖登录状态恢复、Token 存储、购物车 Store、表单校验、优惠券格式化等逻辑。

**AI 提示词：**

```text
请为 frontend 引入 Vitest 单元测试体系。

请先阅读：
- frontend/package.json
- frontend/src/utils/validators.ts
- frontend/src/utils/coupon.ts
- frontend/src/stores/auth.ts
- frontend/src/stores/cart.ts
- frontend/src/api/http.ts

要求：
1. 安装并配置 Vitest、jsdom、@vue/test-utils（如确有需要）。
2. 在 package.json 中增加 test:unit 和 coverage 脚本。
3. 优先补 validators、coupon 工具函数、auth store、cart store 的测试。
4. 对 localStorage、sessionStorage、axios 请求做 mock，不依赖真实后端。
5. 运行 npm run typecheck、npm run test:unit，并输出结果。
```

### T07：建设 Playwright E2E 主链路

**优先级：P0**

E2E 适合展示“真实用户路径自动化”。当前前端页面包括登录、商品浏览、领券中心、我的优惠券、购物车、结算、商家券模板和商品管理。

**建议交付物：**

```text
frontend/playwright.config.ts
frontend/tests/e2e/login.spec.ts
frontend/tests/e2e/user-shopping-flow.spec.ts
frontend/tests/e2e/merchant-coupon-template.spec.ts
docs/testing/e2e-test-guide.md
```

**验收标准：**

- `frontend/package.json` 增加 `test:e2e` 脚本。
- 登录、商品浏览、购物车、结算至少有一条可运行用例。
- 测试账号、测试数据和后端启动方式写入文档。

**AI 提示词：**

```text
请为 frontend 引入 Playwright E2E 测试。

请先阅读：
- frontend/src/router/index.ts
- frontend/src/views/auth/LoginPage.vue
- frontend/src/views/user/ProductBrowsePage.vue
- frontend/src/views/user/CartPage.vue
- frontend/src/views/user/SettlementPage.vue
- docs/frontend-test-data-guide.md
- resources/database/frontend-test-data.sql

要求：
1. 配置 Playwright，并在 package.json 增加 test:e2e 脚本。
2. 编写登录用例，覆盖普通用户和角色跳转。
3. 编写用户购物主链路：登录 -> 商品浏览 -> 加入购物车 -> 购物车汇总 -> 进入结算。
4. 对暂时无法稳定跑通的后端依赖，使用文档记录阻塞项，不要伪造“已通过”。
5. 输出 docs/testing/e2e-test-guide.md，说明启动后端、初始化数据、运行 E2E 的步骤。
```

### T08：建设测试数据初始化和清理体系

**优先级：P1**

项目已有 `resources/database/frontend-test-data.sql` 和 Redis 预热脚本，但还缺少统一的测试数据生命周期管理。

**建议交付物：**

```text
tests/data/init-test-data.sql
tests/data/cleanup-test-data.sql
tests/data/redis-warmup.redis
tests/data/test-accounts.md
docs/testing/test-data-guide.md
```

**验收标准：**

- 测试数据可重复初始化。
- 清理脚本只清理测试数据，不误删业务数据。
- API、E2E、性能测试共用同一套测试账号说明。

**AI 提示词：**

```text
请建设项目测试数据初始化和清理体系。

请先阅读：
- resources/database/frontend-test-data.sql
- resources/database/frontend-test-redis-warmup.redis
- resources/database/*.sql
- docs/frontend-test-data-guide.md

要求：
1. 梳理现有测试账号、商品、优惠券、购物车、用户券数据。
2. 新增 tests/data/init-test-data.sql、cleanup-test-data.sql、redis-warmup.redis、test-accounts.md。
3. 清理脚本必须只删除测试数据，使用固定测试账号、固定 shop_number 或固定 ID 范围限定。
4. 新增 docs/testing/test-data-guide.md，说明初始化、清理、Redis 预热和注意事项。
5. 不写入真实密码明文，测试账号密码说明使用约定值或 Hash 说明。
```

### T09：建设 Testcontainers 集成测试

**优先级：P1**

Redis Lua、ShardingSphere 路由、Elasticsearch 同步、真实 MySQL 写入等内容不适合纯 Mockito。建议用 Testcontainers 建设独立集成测试层。

**建议优先覆盖：**

- Redis Lua 库存扣减和用户领券记录。
- MySQL 分片写入和查询路由。
- Elasticsearch 索引写入、更新、删除。
- MQ 可先做 producer/consumer 的 mock 或本地 profile，不强行一次性引入完整 RocketMQ 容器。

**验收标准：**

- 集成测试默认不跑。
- 可以用明确命令单独执行。
- 每个集成测试都有数据准备和清理。

**AI 提示词：**

```text
请为项目设计并落地第一批 Testcontainers 集成测试。

优先范围：
- Redis Lua 库存扣减相关逻辑
- settlement/engine 中与 Redis 或 MySQL 一致性强相关的 P0 链路

要求：
1. 不影响默认单元测试执行。
2. 新增 integration profile、JUnit Tag 或 *IT 命名。
3. 使用 Testcontainers 启动临时 Redis/MySQL，测试结束自动清理。
4. 先实现 1 到 2 条高价值集成测试，不要试图一次覆盖所有中间件。
5. 更新 docs/testing/integration-test-guide.md，写清楚运行命令和环境要求。
6. 输出验证结果和剩余风险。
```

### T10：补齐幂等、防重和异常响应测试

**优先级：P1**

`framework` 是公共能力模块，幂等和统一异常直接影响多个服务。

**建议交付物：**

```text
framework/src/test/java/com/mall/cqupt/framework/idempotent/NoDuplicateSubmitAspectTest.java
framework/src/test/java/com/mall/cqupt/framework/idempotent/NoMQDuplicateConsumeAspectTest.java
framework/src/test/java/com/mall/cqupt/framework/web/GlobalExceptionHandlerTest.java
```

**验收标准：**

- 覆盖正常执行、重复提交、业务异常释放锁、MQ 重复消费返回等场景。
- 不依赖真实 Redis/Redisson。

**AI 提示词：**

```text
请补齐 framework 模块幂等、防重和统一异常响应测试。

请先阅读：
- framework/src/main/java/com/mall/cqupt/framework/idempotent/*
- framework/src/main/java/com/mall/cqupt/framework/web/GlobalExceptionHandler.java
- framework/src/test/java/com/mall/cqupt/framework/idempotent/SpELUtilTest.java
- framework/src/test/java/com/mall/cqupt/framework/web/ResultsTest.java

要求：
1. 使用 Mockito mock RedissonClient、RLock、StringRedisTemplate。
2. 覆盖重复提交拦截、正常释放锁、异常时释放锁、MQ 重复消费、SpEL key 生成。
3. 覆盖全局异常响应的 code、message 和 success 状态。
4. 运行 framework 模块测试并输出结果。
```

### T11：建设权限和安全测试

**优先级：P1**

项目有平台、商家、普通用户 3 种角色，网关和前端路由都有权限判断。适合做权限矩阵和越权测试。

**建议覆盖：**

- 未登录访问受保护接口。
- 普通用户访问商家接口。
- 商家访问其他店铺数据。
- 平台、商家、用户路由跳转。
- Token 过期、伪造 Token、缺失用户 Header。

**建议交付物：**

```text
docs/testing/security-test-plan.md
tests/api/security.postman_collection.json
gateway/src/test/java/com/mall/cqupt/gateway/filter/TokenValidateFilterTest.java
```

**AI 提示词：**

```text
请建设项目权限和安全测试方案。

要求：
1. 梳理平台、商家、普通用户三类角色的接口权限矩阵。
2. 基于 gateway、merchant-admin、engine、frontend router 设计越权测试用例。
3. 补充 API 自动化集合中的安全负例：缺失 Token、非法 Token、角色越权、跨店铺访问。
4. 如果需要补单元测试，优先扩展 TokenValidateFilterTest 和相关 Service 权限测试。
5. 新增 docs/testing/security-test-plan.md，输出权限矩阵、测试用例和执行方式。
```

### T12：建设性能压测方案

**优先级：P1**

优惠券项目的亮点在高并发领券、库存扣减、购物车汇总和结算计算。性能测试能把项目从“功能自动化”提升到“质量保障”。

**建议场景：**

| 场景 | 核心指标 |
| --- | --- |
| 登录 | TPS、P95、错误率、Redis Session 写入 |
| 同步领券 | 是否超卖、响应时间、库存一致性 |
| 异步领券 | MQ 积压、最终一致性、重复消费 |
| 购物车汇总 | P95、金额正确性、缓存命中 |
| 结算查券 | P95、可用券正确性、金额精度 |

**建议交付物：**

```text
tests/performance/k6/coupon-redeem.js
tests/performance/k6/cart-summary.js
tests/performance/k6/settlement-query.js
docs/testing/performance-test-plan.md
docs/testing/performance-test-report.md
```

**AI 提示词：**

```text
请为项目建设第一版性能压测方案，优先使用 k6。

要求：
1. 先阅读 controller、API 文档和测试数据脚本，确定登录、领券、购物车、结算接口。
2. 新增 tests/performance/k6 下的压测脚本。
3. 脚本支持环境变量配置 baseUrl、账号、并发数、持续时间。
4. 每个脚本至少输出 TPS、P95、错误率，并校验业务 code。
5. 新增 docs/testing/performance-test-plan.md，说明场景、指标、数据准备和风险。
6. 如本地服务未启动，先完成脚本和文档，并说明运行前置条件。
```

### T13：建设搜索同步测试

**优先级：P2**

`search` 模块当前只有 Elasticsearch 连接冒烟测试。后续可以围绕 Canal/MQ 同步构建专项测试。

**建议交付物：**

- Consumer 单元测试。
- Elasticsearch 文档映射测试。
- 索引写入、更新、删除集成测试。
- 搜索一致性测试报告。

**AI 提示词：**

```text
请为 search 模块补齐搜索同步测试设计和首批测试。

请先阅读：
- search/src/main/java/com/mall/cqupt/search/mq/consumer/CanalBinlogSyncCouponTemplateConsumer.java
- search/src/main/java/com/mall/cqupt/search/entity/*
- search/src/test/java/com/mall/cqupt/search/InitElasticsearchTests.java

要求：
1. 先补 Consumer 单元测试，用 Mockito mock ElasticsearchTemplate。
2. 覆盖新增、更新、删除事件的索引行为。
3. 不连接真实 Elasticsearch。
4. 如需要集成测试，隔离为 integration profile。
5. 更新 docs/testing/search-sync-test-plan.md。
```

### T14：建设 CI 测试流水线

**优先级：P1**

CI 是测试开发工程化能力的关键产物。建议在 T01、T04、T06 之后再做。

**建议交付物：**

```text
.github/workflows/test.yml
docs/testing/ci-test-pipeline.md
```

**建议流水线阶段：**

1. 后端编译。
2. 后端稳定单元测试。
3. JaCoCo 覆盖率报告。
4. 前端类型检查。
5. 前端单元测试。
6. 前端构建。
7. 可选：API 自动化、E2E、性能测试作为手动触发或 nightly。

**AI 提示词：**

```text
请为项目建设 CI 测试流水线。

前置要求：
- 默认后端单元测试已经隔离外部依赖。
- 前端已经有 typecheck/build，若已引入 Vitest 则纳入 test:unit。

要求：
1. 新增 .github/workflows/test.yml。
2. 后端使用 JDK 17 和 Maven 缓存。
3. 前端使用 Node.js 20 和 npm 缓存。
4. 默认只跑稳定单元测试、类型检查和构建；API/E2E/性能测试可设计为手动触发或后续阶段。
5. 新增 docs/testing/ci-test-pipeline.md，说明流水线阶段、触发方式、失败排查。
6. 不把真实密钥写进 workflow。
```

### T15：沉淀缺陷闭环和质量报告

**优先级：P2**

测试开发岗位不仅看自动化脚本，也看问题发现、定位、修复和回归闭环。

**建议交付物：**

```text
docs/testing/defect-template.md
docs/testing/regression-checklist.md
docs/testing/test-summary-report.md
docs/testing/quality-dashboard.md
```

**建议记录的案例：**

- 券模板参数校验中 `Integer` 与枚举对象比较导致规则失效。
- 商品券不匹配后继续解引用导致 NPE。
- `applySelectedCoupon` 空实现导致结算链路中断。
- 外部依赖测试导致默认测试不可复现。

**AI 提示词：**

```text
请沉淀项目测试缺陷闭环和质量报告文档。

要求：
1. 基于 docs/testing/test-asset-map.md、risk-matrix.md、backend-unit-test-completion-report.md 梳理已发现问题。
2. 新增 defect-template.md，包含问题描述、环境、复现步骤、期望结果、实际结果、定位过程、修复方案、回归用例。
3. 新增 regression-checklist.md，按模块列出每次修改必须回归的测试。
4. 新增 test-summary-report.md，汇总当前测试资产、覆盖范围、已降低风险、剩余风险和下一步计划。
5. 内容使用中文技术文档风格，避免空泛描述。
```

## 5. 适合写进简历的成果表达

完成上述任务后，可以把项目描述成一个完整测试开发作品，而不是“补了一些测试”。

### 5.1 简历描述示例

```text
基于 Spring Boot + Vue 3 的优惠券商城项目建设自动化测试体系，覆盖登录鉴权、商品管理、优惠券模板、领券、购物车、结算、批量发券等核心链路。负责梳理后端 7 个模块和前端应用的测试资产，建立 P0/P1 风险矩阵；使用 JUnit 5 + Mockito 补齐核心 Service 单元测试，引入 JaCoCo 覆盖率基线；建设 Postman/Newman API 自动化、Playwright E2E、k6 性能压测和 CI 流水线，沉淀测试数据脚本、缺陷闭环模板和质量报告。
```

### 5.2 面试讲解重点

| 主题 | 可以怎么讲 |
| --- | --- |
| 测试策略 | 我先按业务链路和风险分级，而不是按文件数量补测试。P0 是登录、建券、领券、购物车、结算、支付用券和库存一致性。 |
| 自动化分层 | 单元测试覆盖纯业务规则，API 测试覆盖服务接口契约，E2E 覆盖真实用户路径，性能测试覆盖高并发风险。 |
| 中间件测试 | Redis Lua、RocketMQ、Elasticsearch 不放进默认单测，而是隔离为集成测试或专项测试，避免 CI 不稳定。 |
| 缺陷闭环 | 通过测试发现券模板参数校验失效、商品券不匹配 NPE、结算应用券空实现等问题，并补充回归用例。 |
| 工程化 | 用 JaCoCo、CI、测试数据脚本和测试报告把质量工作变成可重复、可度量、可交接的体系。 |

## 6. 不建议让 AI 一次性做的事

- 不要让 AI 一次性“把所有测试都补齐”，容易跨模块大改，难以验证。
- 不要让多个 AI 同时修改同一个模块的 POM、公共测试配置或同一批测试文件。
- 不要让 AI 连接真实外部中间件来“证明测试通过”。
- 不要为了让测试通过而删除现有测试、降低断言或绕过业务校验。
- 不要把 API、E2E、性能测试全部塞进默认 CI；它们应该分层执行。

## 7. 最小可展示闭环

如果只想先做一个能展示的最小闭环，建议按下面 5 个任务执行：

1. T01：隔离不稳定测试，让默认测试可稳定执行。
2. T03：实现 `applySelectedCoupon`，补齐失败和成功单元测试。
3. T04：引入 JaCoCo，生成覆盖率基线。
4. T05：建设登录、领券、购物车、结算 API 自动化集合。
5. T15：输出缺陷闭环和测试总结报告。

这一组任务完成后，你已经可以讲清楚：如何识别风险、如何设计测试、如何发现缺陷、如何修复回归、如何用报告证明质量改进。

