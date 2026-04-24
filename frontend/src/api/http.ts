import axios, { AxiosError, type AxiosResponse } from 'axios'
import JSONBig from 'json-bigint'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useAuthStore } from '@/stores/auth'
import type { ApiResult } from '@/types/auth'

export class ApiError extends Error {
  constructor(
    public code: string,
    message: string,
    public requestId?: string
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

export const http = axios.create({
  baseURL: '/',
  timeout: 10000,
  transformResponse: [
    (data) => {
      if (!data) {
        return data
      }
      return JSONBig({ storeAsString: true }).parse(data)
    }
  ]
})

http.interceptors.request.use((config) => {
  const authStore = useAuthStore()
  if (authStore.token) {
    config.headers.Authorization = `Bearer ${authStore.token}`
  }
  config.headers['X-Request-Source'] = 'onecoupon-web'
  return config
})

const unwrapResponse = (response: AxiosResponse) => {
  const body = response.data as ApiResult<unknown> | unknown
  if (body && typeof body === 'object' && 'code' in body) {
    const result = body as ApiResult<unknown>
    if (result.code === '0') {
      return result.data
    }
    throw new ApiError(result.code, result.message || '请求处理失败', result.requestId)
  }
  return body
}

http.interceptors.response.use(
  unwrapResponse as never,
  async (error: AxiosError<{ message?: string }>) => {
    const authStore = useAuthStore()
    if (error.response?.status === 401) {
      authStore.clearSession()
      ElMessage.warning('登录已过期，请重新登录')
      await router.replace({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
      return Promise.reject(error)
    }
    if (error.code === 'ECONNABORTED') {
      return Promise.reject(new ApiError('NETWORK_TIMEOUT', '请求超时，请检查网络后重试'))
    }
    if (!error.response) {
      return Promise.reject(new ApiError('NETWORK_ERROR', '无法连接后端服务，请确认 merchant-admin 已启动并监听 10010 端口'))
    }
    return Promise.reject(error)
  }
)
