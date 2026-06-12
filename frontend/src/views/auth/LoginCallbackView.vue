<template>
  <div class="callback-container">
    <el-card shadow="hover" class="callback-card">
      <el-icon class="callback-icon" :size="48">
        <Loading v-if="status === 'processing'" />
        <CircleCheck v-else-if="status === 'success'" class="success-icon" />
        <CircleClose v-else class="error-icon" />
      </el-icon>
      <h2 class="callback-title">{{ title }}</h2>
      <p class="callback-subtitle">{{ subtitle }}</p>

      <!-- ========== 处理中：spinner 自动旋转 ========== -->
      <el-progress
        v-if="status === 'processing'"
        :percentage="100"
        :indeterminate="true"
        :duration="2"
        :show-text="false"
        class="callback-progress"
      />

      <!-- ========== 失败：返回登录 ========== -->
      <el-button
        v-if="status === 'error'"
        type="primary"
        size="large"
        class="action-btn"
        @click="goLogin"
      >
        返回登录
      </el-button>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { setToken, setRefreshToken, getToken } from '@/utils/request'
import { useUserStore } from '@/stores/user'

type CallbackStatus = 'processing' | 'success' | 'error'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const status = ref<CallbackStatus>('processing')
const errorMsg = ref<string>('')

/**
 * 标题 / 副标题随状态变化
 */
const title = computed(() => {
  if (status.value === 'processing') return '正在登录...'
  if (status.value === 'success') return '登录成功'
  return '登录失败'
})

const subtitle = computed(() => {
  if (status.value === 'processing') return '正在与企业微信通信并获取用户信息'
  if (status.value === 'success') return `欢迎回来，${userStore.userInfo?.name ?? ''}！`
  return errorMsg.value || '请稍后重试'
})

/**
 * 同源 state 校验：必须以 / 开头且不含 :// (二次防御, 后端已 sanitize)
 */
function isSafeState(s: string | null | undefined): boolean {
  if (!s) return false
  if (!s.startsWith('/')) return false
  if (s.startsWith('//') || s.includes('://')) return false
  return s.length <= 200
}

/**
 * 跳转到 safe state 或默认首页
 */
function goTarget() {
  const state = route.query.state as string | undefined
  if (isSafeState(state)) {
    router.push(state!)
  } else {
    router.push('/')
  }
}

function goLogin() {
  router.push('/login')
}

/**
 * 主流程: 从 URL 拿 token, 存到 localStorage, 拉 /me, 跳原页面
 */
async function processCallback() {
  status.value = 'processing'

  // 1) 拿 token
  const token = route.query.token as string | undefined
  const refreshToken = route.query.refresh_token as string | undefined
  const errorParam = route.query.error as string | undefined

  // 2) 错误优先 (后端 OAuth 失败时跳到 /login/callback?error=xxx, 但实际是 /login?error=...)
  //    此处兼容一下, 如果 token 缺失, 视为失败
  if (!token) {
    errorMsg.value = errorParam || '未携带访问令牌, 请重新登录'
    status.value = 'error'
    return
  }

  // 3) 存 token (access + refresh)
  setToken(token)
  if (refreshToken) {
    setRefreshToken(refreshToken)
  }
  // 重复登录时 request.ts 已经有 token, 不影响

  // 4) 拉 /api/users/me
  try {
    await userStore.fetchUserInfo()
  } catch (e: any) {
    errorMsg.value = e?.message || '获取用户信息失败, 请重试'
    status.value = 'error'
    return
  }

  // 5) 成功提示 + 跳原页面
  ElMessage.success('登录成功')
  status.value = 'success'
  // 给用户 600ms 看到成功状态再跳走
  setTimeout(() => {
    goTarget()
  }, 600)
}

onMounted(() => {
  // 防御: 已有 token 且已登录用户, 不走流程直接跳首页
  if (getToken() && userStore.isLoggedIn) {
    goTarget()
    return
  }
  processCallback()
})
</script>

<style scoped>
.callback-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #67c23a 0%, #409eff 100%);
  padding: 20px;
}

.callback-card {
  width: 100%;
  max-width: 420px;
  border-radius: 12px;
  text-align: center;
  padding: 24px 8px;
}

.callback-icon {
  margin-bottom: 16px;
  color: #909399;
}

.callback-icon.success-icon {
  color: #67c23a;
}

.callback-icon.error-icon {
  color: #f56c6c;
}

.callback-title {
  margin: 0 0 8px 0;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.callback-subtitle {
  margin: 0 0 24px 0;
  color: #606266;
  font-size: 14px;
  min-height: 20px;
}

.callback-progress {
  max-width: 200px;
  margin: 0 auto 24px;
}

.action-btn {
  min-width: 160px;
}
</style>
