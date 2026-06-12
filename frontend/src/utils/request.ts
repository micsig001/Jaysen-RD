import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse
} from 'axios'
import router from '@/router'
import { encrypt, decrypt } from './crypto'

// ============== Token 存储 ==============

const ACCESS_TOKEN_KEY = 'rd_access_token_encrypted'
const REFRESH_TOKEN_KEY = 'rd_refresh_token_encrypted'

/**
 * 安全地获取 Token
 */
export function getToken(): string | null {
  const encrypted = localStorage.getItem(ACCESS_TOKEN_KEY)
  if (!encrypted) return null
  const plain = decrypt(encrypted)
  return plain || null
}

/**
 * 安全地存储 Token
 */
export function setToken(token: string): void {
  if (!token) return
  localStorage.setItem(ACCESS_TOKEN_KEY, encrypt(token))
}

/**
 * 存储 Refresh Token
 */
export function setRefreshToken(token: string): void {
  if (!token) return
  localStorage.setItem(REFRESH_TOKEN_KEY, encrypt(token))
}

/**
 * 清除 Token
 */
export function removeToken(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

// ============== Axios 实例 ==============

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器：附加 Authorization
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：处理 401（跳登录）+ 错误码透传
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    // 后端统一 { code, message, data }，code !== 200 视为业务错误
    if (res?.code !== undefined && res.code !== 200) {
      // 401 未授权：清 token + 跳登录
      if (res.code === 401) {
        removeToken()
        router.push('/login')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    // HTTP 状态码错误
    if (error.response) {
      switch (error.response.status) {
        case 401:
          removeToken()
          router.push('/login')
          break
        case 403:
          console.warn('[request] 403 Forbidden')
          break
        case 404:
          console.warn('[request] 404 Not Found')
          break
        case 500:
          console.error('[request] 500 Server Error')
          break
      }
    }
    return Promise.reject(error)
  }
)

export default service
