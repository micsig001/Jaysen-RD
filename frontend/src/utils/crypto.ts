import CryptoJS from 'crypto-js'

/**
 * AES 加解密工具（用于 Token 本地存储）
 *
 * <p>Phase 1 占位：纯 AES-256-CBC 模式，PKCS7 padding。
 * 实际项目应该用更强的方式（如 Web Crypto API），但 AES 满足"不让 token 明文躺在 localStorage"的基本要求。</p>
 *
 * @author Mavis
 */

const KEY = import.meta.env.VITE_TOKEN_SECRET || 'rd-default-secret-key-32chars-min-padding-x'

/**
 * 加密（明文 → Base64 密文）
 */
export function encrypt(plaintext: string): string {
  if (!plaintext) return ''
  try {
    return CryptoJS.AES.encrypt(plaintext, KEY).toString()
  } catch (e) {
    console.error('[crypto] 加密失败:', e)
    return ''
  }
}

/**
 * 解密（Base64 密文 → 明文）
 */
export function decrypt(ciphertext: string): string {
  if (!ciphertext) return ''
  try {
    const bytes = CryptoJS.AES.decrypt(ciphertext, KEY)
    return bytes.toString(CryptoJS.enc.Utf8)
  } catch (e) {
    console.error('[crypto] 解密失败:', e)
    return ''
  }
}
