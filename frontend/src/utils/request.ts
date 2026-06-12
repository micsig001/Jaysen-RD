import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
  type AxiosResponse
} from 'axios'
import router from '@/router'
import { encrypt, decrypt } from './crypto'

// ============== Token 存储 ==============

const ACCESS_TOKEN_KEY = 'rd_access_token_encrypted'
const REFRESH_TOKEN_KEY = 'rd_refresh_token_encrypted'

/**
 * 安全地获取 Access Token
 */
export function getToken(): string | null {
  const encrypted = localStorage.getItem(ACCESS_TOKEN_KEY)
  if (!encrypted) return null
  const plain = decrypt(encrypted)
  return plain || null
}

/**
 * 安全地获取 Refresh Token
 */
export function getRefreshToken(): string | null {
  const encrypted = localStorage.getItem(REFRESH_TOKEN_KEY)
  if (!encrypted) return null
  const plain = decrypt(encrypted)
  return plain || null
}

/**
 * 安全地存储 Access Token
 */
export function setToken(token: string): void {
  if (token) localStorage.setItem(ACCESS_TOKEN_KEY, encrypt(token))
}

/**
 * 安全地存储 Refresh Token
 */
export function setRefreshToken(token: string): void {
  if (token) localStorage.setItem(REFRESH_TOKEN_KEY, encrypt(token))
}

/**
 * 清除全部 Token
 */
export function removeToken(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

// ============== 401 自动续签（单飞模式） ==============

/**
 * 单例 Promise:并发 401 时只发一个 /auth/refresh
 * 其余请求 await 同一个 Promise 即可共享结果
 */
let refreshInFlight: Promise<string | null> | null = null

/**
 * 调用 /auth/refresh 拿新 access_token
 * 失败返回 null
 */
async function fetchNewAccessToken(): Promise<string | null> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return null

  try {
    // 用裸 axios 避免被本拦截器再次处理
    const baseURL = import.meta.env.VITE_API_URL || '/api'
    const res = await axios.post<{ code: number; data?: { access_token: string } }>(
      `${baseURL}/auth/refresh`,
      { refresh_token: refreshToken },
      { headers: { 'Content-Type': 'application/json' }, timeout: 10000 }
    )
    if (res.data?.code === 200 && res.data.data?.access_token) {
      const newAccess = res.data.data.access_token
      setToken(newAccess)
      return newAccess
    }
    return null
  } catch (e) {
    console.error('[request] refresh 失败', e)
    return null
  }
}

/**
 * 统一处理 401:尝试续签,成功后重放原请求
 * 失败清 token + 跳登录
 */
async function handleUnauthorized(original: InternalAxiosRequestConfig): Promise<AxiosResponse | null> {
  // 单飞
  if (!refreshInFlight) {
    refreshInFlight = fetchNewAccessToken().finally(() => {
      // 500ms 后清空,允许下一次过期重新发起
      setTimeout(() => { refreshInFlight = null }, 500)
    })
  }
  const newToken = await refreshInFlight
  if (!newToken) {
    // refresh 失败,必须登出
    removeToken()
    router.push('/login')
    return null
  }
  // 重放原请求
  original.headers.Authorization = `Bearer ${newToken}`
  return axios.request(original)
}

// ============== Axios 实例 ==============

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器:附加 Authorization
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

// 响应拦截器:处理业务 401 + HTTP 401(自动续签)
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    if (res?.code !== undefined && res.code !== 200) {
      // 业务 401 (例如后端 BizException 抛 401) — 视为过期,清 token 跳登录
      // 不重试 refresh,避免无限循环
      if (res.code === 401) {
        removeToken()
        router.push('/login')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  async (error) => {
    if (error.response) {
      const status = error.response.status
      if (status === 401 && error.config) {
        // HTTP 401:token 过期,尝试续签
        try {
          const retried = await handleUnauthorized(error.config)
          if (retried) {
            // 重放后,统一拆 data
            const res = retried.data
            if (res?.code !== undefined && res.code !== 200) {
              return Promise.reject(new Error(res.message || '请求失败'))
            }
            return res
          }
        } catch (e) {
          return Promise.reject(e)
        }
      } else if (status === 403) {
        console.warn('[request] 403 Forbidden')
      } else if (status === 404) {
        console.warn('[request] 404 Not Found')
      } else if (status === 500) {
        console.error('[request] 500 Server Error')
      }
    }
    return Promise.reject(error)
  }
)

export default service
