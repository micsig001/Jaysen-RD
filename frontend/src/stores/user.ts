import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getCurrentUser, type CurrentUserVO } from '@/api/user'
import { loginByCode, devLogin, type TokenResponse } from '@/api/auth'
import { setToken, setRefreshToken, removeToken } from '@/utils/request'

/**
 * 用户信息（前端 Store 层）
 *
 * <p>对齐后端 {@code CurrentUserVO}，去除手机号/邮箱等敏感字段（Phase 2 暂不做脱敏），
 * 仅保留 WorkbenchLayout 顶栏需要的展示字段。</p>
 */
export interface UserInfo {
  id: number
  userId: string
  name: string
  avatarUrl?: string
  departmentId?: number
  departmentName?: string
  position?: string
  role: 'EMPLOYEE' | 'MANAGER' | 'ADMIN'
  status: number
  lastLoginAt?: string
}

/**
 * 用户状态管理
 */
export const useUserStore = defineStore('user', () => {
  const userInfo = ref<UserInfo | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')
  const isManager = computed(
    () => userInfo.value?.role === 'MANAGER' || userInfo.value?.role === 'ADMIN'
  )
  const isLoggedIn = computed(() => userInfo.value != null)

  /**
   * 把后端 CurrentUserVO 转成 Store 内部 UserInfo
   */
  function toUserInfo(vo: CurrentUserVO): UserInfo {
    return {
      id: vo.id,
      userId: vo.userId,
      name: vo.name,
      avatarUrl: vo.avatarUrl,
      departmentId: vo.departmentId,
      departmentName: vo.departmentName,
      position: vo.position,
      role: vo.role,
      status: vo.status,
      lastLoginAt: vo.lastLoginAt
    }
  }

  function setUserInfo(info: UserInfo) {
    userInfo.value = info
  }

  function clearUserInfo() {
    userInfo.value = null
    error.value = null
  }

  /**
   * 把 TokenResponse 落盘到 localStorage（加密存储）
   */
  function applyTokens(token: TokenResponse) {
    setToken(token.access_token)
    if ('refresh_token' in token && (token as any).refresh_token) {
      setRefreshToken((token as any).refresh_token)
    }
  }

  /**
   * 拉取当前登录用户信息（GET /api/users/me）
   *
   * <p>通常在应用启动时（WorkbenchLayout.onMounted）调一次，
   * 拿到用户信息后 Store 才有 role / isAdmin 等计算属性，路由守卫才能用。</p>
   */
  async function fetchUserInfo() {
    loading.value = true
    error.value = null
    try {
      const res = await getCurrentUser()
      // request.ts 拦截器已解包 Result.data，调用方拿到的是 CurrentUserVO
      // 但 TS 类型上 request<T> 返回 AxiosResponse<T>，所以需要 .data
      setUserInfo(toUserInfo(res.data))
      return userInfo.value
    } catch (e: any) {
      error.value = e?.message || '获取用户信息失败'
      // 401 已经由 request.ts 拦截器清 token + 跳 /login
      clearUserInfo()
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * dev 环境：用 userId 直登（POST /api/auth/dev-login）
   *
   * <p>仅后端 {@code @Profile("dev")} 暴露，本地无企微 corpId 时的兜底登录入口。</p>
   */
  async function loginDev(userId: string) {
    loading.value = true
    error.value = null
    try {
      const res = await devLogin(userId)
      applyTokens(res.data)
      await fetchUserInfo()
      return userInfo.value
    } catch (e: any) {
      error.value = e?.message || '登录失败'
      removeToken()
      clearUserInfo()
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * 企微授权码换 Token（POST /api/auth/token）
   *
   * <p>前端拿到企微回调的 code 后调用。Token 落盘后立即拉取用户信息。</p>
   */
  async function loginWithWeWorkCode(code: string) {
    loading.value = true
    error.value = null
    try {
      const res = await loginByCode(code)
      applyTokens(res.data)
      await fetchUserInfo()
      return userInfo.value
    } catch (e: any) {
      error.value = e?.message || '登录失败'
      removeToken()
      clearUserInfo()
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * 退出登录
   *
   * <p>仅清前端 store + token；后端 logout 端点（POST /api/auth/logout）由调用方触发，
   * 这里不强制等响应（黑名单是非关键路径）。</p>
   */
  function logoutLocal() {
    removeToken()
    clearUserInfo()
  }

  return {
    userInfo,
    loading,
    error,
    isAdmin,
    isManager,
    isLoggedIn,
    setUserInfo,
    clearUserInfo,
    fetchUserInfo,
    loginDev,
    loginWithWeWorkCode,
    logoutLocal
  }
})
