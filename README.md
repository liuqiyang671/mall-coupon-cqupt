# OneCoupon 邮惠券营销系统

<p align="center">
  <strong>面向多角色的优惠券管理与电商平台</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange" alt="Java 17" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.0.7-green" alt="Spring Boot 3.0.7" />
  <img src="https://img.shields.io/badge/Spring%20Cloud-2022.0.3-blue" alt="Spring Cloud 2022.0.3" />
  <img src="https://img.shields.io/badge/Vue-3.5-brightgreen" alt="Vue 3" />
  <img src="https://img.shields.io/badge/TypeScript-5.7-blue" alt="TypeScript" />
  <img src="https://img.shields.io/badge/ShardingSphere-5.3.2-red" alt="ShardingSphere" />
  <img src="https://img.shields.io/badge/License-Apache--2.0-yellow" alt="License" />
</p>

---

## 目录

- [项目概述](#项目概述)
- [功能特性](#功能特性)
- [技术架构](#技术架构)
- [环境配置要求](#环境配置要求)
- [安装步骤](#安装步骤)
- [启动指南](#启动指南)
- [使用说明](#使用说明)
- [目录结构](#目录结构)
- [贡献指南](#贡献指南)
- [许可证](#许可证)

---

## 项目概述

**OneCoupon 邮惠券营销系统**是一个基于 Spring Cloud 微服务架构的优惠券管理与电商平台，面向三种角色（平台人员、商家、普通用户），提供完整的优惠券生命周期管理、商品管理、购物车、结算用券等功能。

系统采用前后端分离架构，后端 6 个业务微服务 + 1 个公共框架模块，通过 Spring Cloud Gateway 统一对外暴露 API；前端基于 Vue 3 + TypeScript 构建，通过 Axios 调用后端微服务。

### 角色体系

| 角色 | roleType | 核心权限 |
|------|----------|---------|
| 平台人员 | 0 | 查看全部商家数据、审核商家、管理商品分类和属性 |
| 商家 | 1 | 管理自身店铺商品、创建/管理优惠券模板、创建推送任务 |
| 普通用户 | 2 | 领券中心领券、管理个人优惠券、购物车、结算用券、预约提醒 |

---

## 功能特性

### 优惠券管理
- **模板创建**：支持立减券、满减券、折扣券三种类型，责任链参数验证
- **领券中心**：同步事务领券 + MQ 异步领券双模式，Lua 脚本原子扣减库存
- **批量发券**：Excel 导入用户列表，MQ 延迟消息 + 批量扣减库存
- **预约提醒**：位图存储提醒信息，MQ 延迟消息到期触发
- **结算用券**：策略模式计算折扣（立减/满减/折扣），CompletableFuture 并行查询

### 商品与购物车
- **商品管理**：三级分类树、多图上传、自定义属性、库存管理（乐观锁）
- **购物车**：添加/删除/修改数量、选中/全选、价格汇总计算（含税费）
- **分库分表**：ShardingSphere 按 shop_number/user_id 哈希分片，520+ 物理表

### 安全与高可用
- **JWT 认证**：HMAC256 签名，Gateway 统一拦截，Token Redis 会话管理
- **防重复提交**：Redisson 分布式锁 + 请求路径 + 参数 MD5
- **MQ 幂等消费**：Redis SETNX + SpEL Key
- **布隆过滤器**：防缓存穿透
- **登录防暴力破解**：Redis 失败计数 + 锁定机制

### 搜索与同步
- **ES 搜索**：Elasticsearch 全文检索优惠券模板
- **Canal 数据同步**：监听 MySQL Binlog，MQ 投递同步到 ES

---

## 技术架构

```
                       ┌─────────────┐
                       │   Nacos     │ (服务注册发现)
                       └──────┬──────┘
                              │
┌──────────┐    ┌─────────────┴──────────────┐
│  前端     │───▶│       Gateway (10000)       │
│ Vue 3+TS │    │  JWT认证 / 路由转发 / 日志    │
└──────────┘    └─────────────┬──────────────┘
                              │
             ┌────────────────┼────────────────┐
             │                │                │
    ┌────────▼──────┐ ┌──────▼──────┐ ┌───────▼──────┐
    │Merchant-Admin │ │   Engine    │ │  Settlement  │
    │   (10010)     │ │  (10020)    │ │  (10030)     │
    └──────┬───────┘ └──────┬──────┘ └───────┬──────┘
           │                │                 │
    ┌──────▼───────┐ ┌──────▼──────┐
    │ Distribution │ │   Search    │
    │  (10040)     │ │  (10050)    │
    └──────────────┘ └─────────────┘
```

### 技术栈总览

| 层次 | 技术 | 版本 |
|------|------|------|
| 后端语言 | Java | 17 |
| 核心框架 | Spring Boot | 3.0.7 |
| 微服务 | Spring Cloud | 2022.0.3 |
| 注册中心 | Spring Cloud Alibaba (Nacos) | 2022.0.0.0-RC2 |
| 网关 | Spring Cloud Gateway | — |
| ORM | MyBatis-Plus | 3.5.7 |
| 分库分表 | Apache ShardingSphere | 5.3.2 |
| 缓存 | Redis + Redisson | 3.27.2 |
| 消息队列 | Apache RocketMQ | 2.3.0 Starter |
| 搜索引擎 | Elasticsearch | — |
| 数据同步 | Canal | — |
| 认证 | JWT (HMAC256) | — |
| API 文档 | Knife4j (OpenAPI 3) | 4.5.0 |
| Excel | EasyExcel | 4.0.1 |
| 工具库 | Hutool | 5.8.27 |
| JSON | Fastjson2 | 2.0.36 |
| 前端框架 | Vue 3 | ^3.5.13 |
| 构建工具 | Vite | ^6.0.5 |
| 前端语言 | TypeScript | ^5.7.2 |
| 状态管理 | Pinia | ^2.3.0 |
| 路由 | Vue Router | ^4.5.0 |
| UI 组件库 | Element Plus | ^2.9.3 |
| HTTP 客户端 | Axios | ^1.7.9 |

---

## 环境配置要求

### 必需环境

| 组件 | 最低版本 | 推荐版本 | 说明 |
|------|---------|---------|------|
| **JDK** | 17 | 17+ | 项目使用 Java 17 特性，必须使用 JDK 17 |
| **Maven** | 3.8+ | 3.9.x | 后端项目构建工具 |
| **Node.js** | 18+ | 20.x LTS | 前端开发环境 |
| **npm** | 9+ | 10+ | 随 Node.js 安装 |
| **MySQL** | 8.0+ | 8.0.x | 数据库，需支持 JSON 字段类型 |
| **Redis** | 6.0+ | 7.x | 缓存、分布式锁、会话管理 |

### 可选环境（按需启用）

| 组件 | 版本 | 说明 |
|------|------|------|
| **Nacos** | 2.x | 服务注册发现（默认关闭，`spring.cloud.nacos.discovery.enabled=false`） |
| **RocketMQ** | 5.x | 消息队列（领券异步模式、批量发券、预约提醒、ES 同步） |
| **Elasticsearch** | 8.x | 优惠券模板全文检索 |
| **Canal** | 1.1.x | MySQL Binlog 监听，同步数据到 ES |
| **XXL-Job** | 2.4.x | 分布式任务调度（默认关闭，`xxl-job.enable=false`） |

### 开发工具

| 工具 | 说明 |
|------|------|
| IntelliJ IDEA | 后端开发 IDE，需安装 Lombok 插件 |
| VS Code / WebStorm | 前端开发 IDE |
| Navicat / DBeaver | 数据库管理工具 |
| Redis Desktop Manager | Redis 可视化管理 |

---

## 安装步骤

### 1. 克隆项目

```bash
git clone https://github.com/<your-username>/mall-cqupt-lqy11.git
cd mall-cqupt-lqy11
```

### 2. 数据库初始化

创建两个分片数据库并执行建表脚本：

```bash
# 登录 MySQL
mysql -u root -p

# 执行数据库初始化脚本（包含分库分表建表语句）
source resources/database/onecoupon.sql;
source resources/database/user_account.sql;
source resources/database/goods.sql;
source resources/database/cart.sql;
source resources/database/coupon_template.sql;
source resources/database/coupon.sql;
source resources/database/coupon_log.sql;
source resources/database/coupon_task.sql;
source resources/database/coupon_user_coupon01.sql;
source resources/database/coupon_user_coupon02.sql;
```

这将创建以下数据库：
- `mall_coupon_cqupt_0`（或 `one_coupon_0`）
- `mall_coupon_cqupt_1`（或 `one_coupon_1`）

### 3. Redis 配置

确保 Redis 服务已启动，默认配置为 `127.0.0.1:6379`。如需修改密码，请编辑各模块的 `application.yaml`：

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: your_password
```

### 4. 后端依赖安装

```bash
# 在项目根目录执行 Maven 构建
mvn clean install -DskipTests
```

### 5. 前端依赖安装

```bash
cd frontend
npm install
```

---

## 启动指南

### 后端服务启动

各微服务需按顺序启动，建议启动顺序如下：

#### 第一步：启动基础服务

确保以下中间件已启动：
- MySQL（端口 3306）
- Redis（端口 6379）

#### 第二步：启动微服务

每个服务在 IDE 中运行对应的 `*Application.java` 主类，或使用 Maven 命令：

```bash
# 1. 启动网关（必须第一个启动）
cd gateway
mvn spring-boot:run

# 2. 启动商家管理模块
cd merchant-admin
mvn spring-boot:run

# 3. 启动核心引擎模块
cd engine
mvn spring-boot:run

# 4. 启动结算模块
cd settlement
mvn spring-boot:run

# 5. 启动分发模块（需 RocketMQ）
cd distribution
mvn spring-boot:run

# 6. 启动搜索模块（需 Elasticsearch）
cd search
mvn spring-boot:run
```

#### 服务端口映射

| 服务 | 端口 | 启动类 | 必需 |
|------|------|--------|------|
| Gateway | 10000 | `GatewayApplication` | 是 |
| Merchant-Admin | 10010 | `MerchantAdminApplication` | 是 |
| Engine | 10020 | `EngineApplication` | 是 |
| Settlement | 10030 | `SettlementApplication` | 是 |
| Distribution | 10040 | `DistributionApplication` | 否（需 RocketMQ） |
| Search | 10050 | `SearchApplication` | 否（需 ES） |

### 前端启动

#### 开发环境

```bash
cd frontend

# 启动开发服务器（默认 http://localhost:5173）
npm run dev
```

前端开发服务器已配置 Vite 代理，自动将 API 请求转发到对应后端服务：

| 代理路径 | 目标服务 |
|---------|---------|
| `/api/merchant-admin` | http://localhost:10010 |
| `/api/user` | http://localhost:10010 |
| `/api/engine` | http://localhost:10020 |
| `/api/settlement` | http://localhost:10030 |
| `/api/distribution` | http://localhost:10040 |
| `/api/search` | http://localhost:10050 |

#### 生产环境构建

```bash
cd frontend

# 类型检查
npm run typecheck

# 构建生产包（输出到 dist/ 目录）
npm run build

# 本地预览构建结果
npm run preview
```

构建产物部署到 Nginx 或其他 Web 服务器，需配置反向代理将 `/api/*` 请求转发到 Gateway（10000 端口）。

### 配置文件说明

各模块关键配置文件：

| 文件 | 模块 | 说明 |
|------|------|------|
| `application.yaml` | 各模块 | 主配置（端口、数据源、Redis、MQ） |
| `shardingsphere-config.yaml` | merchant-admin / engine / settlement / distribution | ShardingSphere 分片规则 |
| `vite.config.ts` | frontend | Vite 构建配置 + API 代理 |

#### ShardingSphere 数据源配置

修改各模块 `shardingsphere-config.yaml` 中的数据库连接信息：

```yaml
dataSources:
  ds_0:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://127.0.0.1:3306/mall_coupon_cqupt_0?useUnicode=true&characterEncoding=UTF-8&rewriteBatchedStatements=true&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
  ds_1:
    # 同上，指向 mall_coupon_cqupt_1
```

---

## 使用说明

### API 文档

启动后端服务后，访问 Knife4j 在线 API 文档：

| 服务 | 文档地址 |
|------|---------|
| Merchant-Admin | http://localhost:10010/doc.html |
| Engine | http://localhost:10020/doc.html |
| Search | http://localhost:10050/doc.html |

### 前端页面

启动前端后访问 http://localhost:5173，系统提供以下页面：

#### 认证页面
- **登录**：三角色切换登录（平台/商家/用户）
- **注册**：三角色注册，商家角色需填写店铺编号

#### 商家/平台后台
- **工作台**：模块入口导航，角色动态展示
- **优惠券模板管理**：创建/编辑/查看模板，上架/下架，增发量
- **推送任务管理**：创建批量发券任务，任务状态跟踪
- **商品管理**：商品 CRUD、分类管理、属性管理、库存调整

#### 用户端
- **领券中心**：浏览可领取优惠券，同步/MQ 异步领券
- **我的优惠券**：按状态筛选（未使用/已使用/已过期/已锁定/已撤回）
- **预约提醒**：创建/查看/取消预约提醒
- **购物车**：添加/删除商品、数量修改、选中/全选、价格汇总
- **优惠券结算**：模拟下单、可用券推荐、折扣计算

### 核心业务流程

```
商家创建优惠券模板 → Redis 预热缓存
    → 用户在领券中心领取（Lua 原子扣减库存）
    → 用户添加商品到购物车
    → 用户结算时选择优惠券（策略模式计算折扣）
    → 创建结算单 → 核销/退款
```

---

## 目录结构

```
mall-cqupt-lqy11/
├── gateway/                    # 网关模块 (端口 10000)
│   └── src/main/java/.../gateway/
│       ├── filter/             # JWT 认证过滤器、请求日志过滤器
│       └── GatewayApplication.java
│
├── framework/                  # 公共框架模块（不可独立运行）
│   └── src/main/java/.../framework/
│       ├── config/             # 缓存、幂等、Web 自动配置
│       ├── exception/          # 统一异常体系
│       ├── idempotent/         # 防重复提交、MQ 幂等消费
│       ├── result/             # 统一返回格式 Result<T>
│       └── web/                # 全局异常处理器
│
├── merchant-admin/             # 商家管理模块 (端口 10010)
│   └── src/main/java/.../merchant/admin/
│       ├── controller/         # 用户、模板、任务、商品、分类、属性 Controller
│       ├── dao/                # 实体类、Mapper、分片算法
│       └── service/            # 业务逻辑层
│   └── src/main/resources/
│       ├── application.yaml
│       ├── shardingsphere-config.yaml
│       └── mapper/             # MyBatis XML
│
├── engine/                     # 核心引擎模块 (端口 10020)
│   └── src/main/java/.../engine/
│       ├── controller/         # 模板查询、用户券、预约提醒、购物车 Controller
│       ├── dao/                # 实体类、Mapper、分片算法
│       ├── service/            # 业务逻辑层
│       └── toolkit/            # 工具类
│   └── src/main/resources/
│       ├── application.yaml
│       ├── shardingsphere-config.yaml
│       ├── lua/                # Redis Lua 脚本（库存扣减等）
│       └── mapper/
│
├── settlement/                 # 结算模块 (端口 10030)
│   └── src/main/java/.../settlement/
│       ├── controller/         # 优惠券应用、查询 Controller
│       ├── dao/                # 实体类、Mapper、分片算法
│       ├── service/            # 业务逻辑 + 策略模式折扣计算
│       └── handler/            # 异步响应处理器
│   └── src/main/resources/
│       ├── application.yaml
│       ├── shardingsphere-config.yaml
│       └── mapper/
│
├── distribution/               # 分发模块 (端口 10040)
│   └── src/main/java/.../distribution/
│       ├── service/            # MQ 消费者、消息发送策略
│       └── dao/                # 实体类、Mapper、分片算法
│   └── src/main/resources/
│       ├── application.yaml
│       ├── shardingsphere-config.yaml
│       ├── lua/                # 批量操作 Lua 脚本
│       └── mapper/
│
├── search/                     # 搜索模块 (端口 10050)
│   └── src/main/java/.../search/
│       ├── entity/             # ES 文档实体
│       └── SearchApplication.java
│   └── src/main/resources/
│       └── application.yaml
│
├── frontend/                   # 前端项目
│   ├── src/
│   │   ├── api/                # API 请求封装
│   │   ├── components/         # 公共组件（认证、权限守卫）
│   │   ├── layouts/            # 布局组件（AppShell、AuthLayout）
│   │   ├── router/             # 路由配置
│   │   ├── stores/             # Pinia 状态管理（auth、cart）
│   │   ├── types/              # TypeScript 类型定义
│   │   ├── utils/              # 工具函数
│   │   └── views/              # 页面组件
│   │       ├── auth/           # 登录、注册、找回密码
│   │       ├── merchant/       # 商家后台页面
│   │       └── user/           # 用户端页面
│   ├── index.html
│   ├── package.json
│   ├── tsconfig.json
│   └── vite.config.ts          # Vite 配置 + API 代理
│
├── resources/                  # 数据库脚本
│   └── database/
│       ├── onecoupon.sql       # 主建表脚本（含分片表）
│       ├── user_account.sql    # 用户表
│       ├── goods.sql           # 商品相关表
│       ├── cart.sql            # 购物车表
│       ├── coupon_template.sql # 优惠券模板表
│       ├── coupon.sql          # 优惠券表
│       ├── coupon_log.sql      # 操作日志表
│       ├── coupon_task.sql     # 推送任务表
│       ├── coupon-data.sql     # 测试数据
│       └── goods-cart-sharding.sql  # 商品与购物车分库分表方案
│
├── docs/                       # 项目文档
│   └── 前端开发技术文档/        # 前端模块技术文档
│
└── pom.xml                     # Maven 父 POM（版本管理）
```

---

## 贡献指南

欢迎对本项目做出贡献！请遵循以下流程：

### 开发环境准备

1. Fork 本仓库到你的 GitHub 账号
2. Clone Fork 后的仓库到本地
3. 确保 JDK 17、Maven 3.8+、Node.js 18+ 已安装
4. 按照上述 [安装步骤](#安装步骤) 配置开发环境

### 开发流程

1. **创建分支**：从 `main` 分支创建功能分支
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **编码规范**：
   - 后端遵循阿里巴巴 Java 开发手册
   - 使用 Lombok 注解简化 POJO
   - Controller 方法需添加 Swagger 注解
   - 新增接口需实现幂等性（`@NoDuplicateSubmit` 或 `@NoMQDuplicateConsume`）
   - 前端使用 TypeScript 严格模式，组件使用 Vue 3 Composition API

3. **提交代码**：
   ```bash
   git add .
   git commit -m "feat: 简短描述本次变更"
   ```
   提交信息格式遵循 [Conventional Commits](https://www.conventionalcommits.org/)：
   - `feat:` 新功能
   - `fix:` 修复 Bug
   - `docs:` 文档变更
   - `refactor:` 代码重构
   - `test:` 测试相关
   - `chore:` 构建/工具变更

4. **推送并创建 PR**：
   ```bash
   git push origin feature/your-feature-name
   ```
   在 GitHub 上创建 Pull Request，描述变更内容和原因。

### 代码审查要点

- 分片键是否正确传递（查询条件必须包含分片键）
- 是否存在跨分片 JOIN（应改为应用层组装）
- Redis 缓存是否设置合理 TTL
- 接口是否实现幂等性
- 前端是否处理了 BigInt 精度问题（使用 json-bigint）

---

## 许可证

本项目基于 [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) 许可证开源。

---

<p align="center">
  OneCoupon 邮惠券营销系统 &copy; 2026
</p>
