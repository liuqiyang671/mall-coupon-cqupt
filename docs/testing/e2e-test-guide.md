# 前端 E2E 测试指南

## 测试范围

本次为 `frontend` 引入 Playwright E2E 测试，覆盖：

| 用例文件 | 覆盖内容 |
| --- | --- |
| `frontend/tests/e2e/auth.spec.ts` | 普通用户登录、商家登录、普通用户访问商家路由后的角色跳转。 |
| `frontend/tests/e2e/user-shopping.spec.ts` | 普通用户登录、商品浏览、加入购物车、购物车汇总、进入结算页。 |

测试不 mock 后端接口，默认使用 Vite dev server 代理真实后端：

| 前端路径 | 后端服务 |
| --- | --- |
| `/api/merchant-admin/**`、`/api/user/**` | `http://localhost:10010` |
| `/api/engine/**` | `http://localhost:10020` |
| `/api/settlement/**` | `http://localhost:10030` |

## 文件变更

| 文件 | 说明 |
| --- | --- |
| `frontend/playwright.config.ts` | Playwright 配置，自动启动 `npm run dev`，默认端口 `5174`。 |
| `frontend/package.json` | 新增 `test:e2e` 脚本。 |
| `frontend/tests/e2e/**` | E2E 用例、登录辅助方法和种子账号配置。 |
| `frontend/src/views/**`、`frontend/src/components/auth/PasswordField.vue` | 新增少量 `data-testid`，用于稳定定位关键控件。 |

## 环境准备

1. 安装前端依赖。

   ```powershell
   cd frontend
   npm install
   ```

2. 安装 Playwright Chromium 浏览器。

   ```powershell
   cd frontend
   npx playwright install chromium
   ```

3. 初始化 MySQL 表结构和前端测试数据。

   建表脚本顺序参考 `docs/frontend-test-data-guide.md`。完成建表后执行：

   ```powershell
   mysql -uroot -p --default-character-set=utf8mb4 < resources/database/frontend-test-data.sql
   ```

4. 预热 Redis。

   ```powershell
   $utf8 = New-Object System.Text.UTF8Encoding($false)
   $OutputEncoding = $utf8
   [Console]::OutputEncoding = $utf8
   Get-Content -Encoding UTF8 resources/database/frontend-test-redis-warmup.redis |
     Where-Object { $_.Trim() -and -not $_.Trim().StartsWith('#') } |
     redis-cli -h 127.0.0.1 -p 6379 -a Lqy259931 --pipe
   ```

5. 启动后端依赖和服务。

   至少需要：

   | 依赖或服务 | 用途 |
   | --- | --- |
   | MySQL | 登录、商品、购物车、用户券、结算数据。 |
   | Redis | 登录 token、商品缓存、券模板缓存、购物车缓存。 |
   | RocketMQ | 领券等链路依赖，当前 E2E 主链路不直接领券，但完整环境建议启动。 |
   | `merchant-admin:10010` | 登录、用户信息、用户商品浏览。 |
   | `engine:10020` | 购物车。 |
   | `settlement:10030` | 查询可用券、结算页计算。 |

   示例：

   ```powershell
   mvn -pl merchant-admin -am spring-boot:run
   mvn -pl engine -am spring-boot:run
   mvn -pl settlement -am spring-boot:run
   ```

## 执行命令

在 `frontend` 目录执行：

```powershell
npm run test:e2e
```

默认情况下，Playwright 会在 `127.0.0.1:5174` 启动独立 Vite dev server，避免误连本机已有的 `5173` 服务。需要复用已有前端服务时：

```powershell
$env:E2E_FRONTEND_PORT="5173"
$env:E2E_REUSE_SERVER="true"
npm run test:e2e
```

只跑登录与角色跳转：

```powershell
npx playwright test tests/e2e/auth.spec.ts
```

只跑用户购物主链路：

```powershell
npx playwright test tests/e2e/user-shopping.spec.ts
```

调试模式：

```powershell
npx playwright test --headed --debug
```

如果 `npx playwright install chromium` 下载失败，但本机已经安装 Chrome 或 Edge，可以临时使用系统浏览器：

```powershell
$env:E2E_BROWSER_CHANNEL="chrome"
npm run test:e2e
```

或：

```powershell
$env:E2E_BROWSER_CHANNEL="msedge"
npm run test:e2e
```

查看报告：

```powershell
npx playwright show-report
```

## 账号与种子数据

测试账号来自 `resources/database/frontend-test-data.sql`：

| 场景 | 用户名 | 密码 | 角色 |
| --- | --- | --- | --- |
| 商家登录 | `merchant01` | `Test123456` | 商家 |
| 普通用户登录 | `customer01` | `Test123456` | 用户 |

用户购物主链路依赖商品 `800001`，其名称包含 `CQUPT-GOODS-001`，并要求该商品已经在 Redis 商品缓存中预热。

## 当前阻塞项

E2E 是真实后端联调测试。若本地没有启动 `10010`、`10020`、`10030`，测试会在登录、商品列表、购物车或结算接口处失败。该失败代表环境未准备完成，不应记录为 E2E 通过。

已知风险：

- `settlement` 结算页依赖 `/api/settlement/coupon-query`，Redis 或用户券种子数据缺失会导致可用券列表为空。
- 购物车加购会写入真实数据库，重复执行会增加商品 `800001` 的购物车数量；需要可重复基线时，重新导入 `frontend-test-data.sql`。
- 当前测试不覆盖“确认使用优惠券”按钮的成功提交，因为该链路依赖 settlement 应用优惠券接口和用户券状态，建议在后端稳定后单独补充。
