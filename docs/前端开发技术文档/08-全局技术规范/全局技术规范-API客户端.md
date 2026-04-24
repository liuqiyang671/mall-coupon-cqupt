# 全局技术规范-API客户端

## 1. 目标

统一 Axios 客户端、认证头、大整数处理、错误处理和重试策略，避免各模块重复实现。

## 2. 基础配置

```ts
import axios from 'axios'
import JSONBig from 'json-bigint'

export const http = axios.create({
  baseURL: '/',
  timeout: 10000,
  transformResponse: [(data) => {
    if (!data) return data
    return JSONBig({ storeAsString: true }).parse(data)
  }]
})
```

## 3. 请求拦截

```ts
http.interceptors.request.use((config) => {
  const token = authStore.token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  config.headers['X-Request-Source'] = 'onecoupon-web'
  return config
})
```

## 4. 响应处理

| 类型 | 判断 | 处理 |
| --- | --- | --- |
| 成功 | `code === "0"` 或 HTTP 2xx 特殊接口 | 返回 `data` |
| 未登录 | HTTP 401 或登录过期文案 | 清除 Token，跳转登录 |
| 业务错误 | `code` 非 0 | 抛出业务异常 |
| 网络错误 | 无响应 | Toast + 可重试 |

```ts
http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && Object.prototype.hasOwnProperty.call(body, 'code')) {
      if (body.code === '0') return body.data
      throw new ApiError(body.code, body.message, body.requestId)
    }
    return body
  },
  (error) => Promise.reject(normalizeHttpError(error))
)
```

## 5. 接口命名

```text
src/api/
├── auth-api.ts
├── coupon-template-api.ts
├── coupon-task-api.ts
├── coupon-center-api.ts
├── coupon-remind-api.ts
└── settlement-api.ts
```

每个 API 文件只封装对应模块接口，禁止跨模块混写。

## 6. 错误分类

| 类别 | 示例 | UI |
| --- | --- | --- |
| `ValidationError` | 前端表单不合法 | 字段错误 |
| `BusinessError` | 后端 `A000001` | Toast/局部提示 |
| `AuthError` | 401 | 登录跳转 |
| `NetworkError` | timeout | Toast + 重试 |
| `ServerError` | 5xx | Toast + 记录日志 |

## 7. 大整数策略

后端 ID 多为 Java `Long`，前端必须按字符串处理：

- 路由参数使用字符串。
- 表格 `rowKey` 使用字符串。
- 禁止对 ID 做 `Number(id)`。

## 8. 安全策略

- Token 不写入 URL。
- 日志禁止打印完整 Token。
- 退出登录后清理 Pinia、localStorage、sessionStorage 中认证字段。
