import request from '@/utils/request'

/**
 * 认证 API
 *
 * 后端接口：
 *   POST /api/auth/token           — 用企微 code 换 JWT
 *   GET  /api/auth/wework/callback — 企微 OAuth 回调（GET，重定向用，前端一般不直接调）
 *   POST /api/auth/refresh         — 用 refresh_token 换新 access_token
 *   POST /api/auth/logout          — 退出登录（token 进黑名单）
 *
 * 另有一个 dev-only 端点（profile=dev 时暴露）：
 *   POST /api/auth/dev-login       — 本地绕过 OAuth 直接登录
 */

/**
 * 登录响应 —— 后端 {@code AuthController.getToken/refreshToken} 返回
 */
export interface TokenResponse {
  access_token: string
  token_type: 'Bearer'
}

/**
 * 用企微授权码换 JWT
 *
 * @param code 企业微信回调的 code（30 分钟有效，单次使用）
 */
export function loginByCode(code: string) {
  return request<TokenResponse>({ url: '/auth/token', method: 'post', data: { code } })
}

/**
 * 刷新 Token
 *
 * @param refreshToken refresh_token（access_token 过期后用）
 */
export function refreshToken(refreshToken: string) {
  return request<TokenResponse>({ url: '/auth/refresh', method: 'post', data: { refresh_token: refreshToken } })
}

/**
 * 退出登录（后端把当前 token 加黑名单）
 *
 * <p>注意：调用方需在请求头带 Authorization Bearer；request.ts 拦截器已自动附加。</p>
 */
export function logout() {
  return request<void>({ url: '/auth/logout', method: 'post' })
}

/**
 * dev 环境绕过企微 OAuth，直接登录
 *
 * <p>仅在 VITE_PROFILE=dev 时后端暴露（{@code DevAuthController} 类级 {@code @Profile("dev")}）。</p>
 *
 * @param userId 企微 userId（数据库里已存在的）
 */
export function devLogin(userId: string) {
  return request<TokenResponse>({ url: '/auth/dev-login', method: 'post', data: { userId } })
}
