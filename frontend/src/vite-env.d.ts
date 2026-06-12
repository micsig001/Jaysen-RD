/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_URL: string
  readonly VITE_WS_URL: string
  readonly VITE_TOKEN_SECRET: string
  /** 企业微信 corpId（用于前端构造 OAuth 授权 URL） */
  readonly VITE_WEWORK_CORP_ID: string
  /** 企业微信应用 agentId（用于 OAuth redirect_uri 中的 appid 区分） */
  readonly VITE_WEWORK_AGENT_ID: string
  /** OAuth 回调地址（前端发起授权时携带的 redirect_uri），指向后端 */
  readonly VITE_WEWORK_OAUTH_REDIRECT_URI: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
