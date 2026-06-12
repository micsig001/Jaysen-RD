<template>
  <div class="login-container">
    <el-card shadow="hover" class="login-card">
      <div class="login-header">
        <h1 class="login-title">仪器类产品研发管理系统</h1>
        <p class="login-subtitle">Instrument R&amp;D Management System</p>
      </div>

      <el-divider />

      <!-- ========== 主登录区：dev 直登（Phase 2 暂唯一可跑通路径） ========== -->
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="企微 UserID" prop="userId">
          <el-input
            v-model="form.userId"
            placeholder="请输入企微 UserID（dev 模式直接登录）"
            clearable
            :prefix-icon="User"
            size="large"
            autocomplete="off"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="userStore.loading"
            class="login-btn"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>

        <el-alert
          v-if="userStore.error"
          :title="userStore.error"
          type="error"
          :closable="false"
          show-icon
          class="error-alert"
        />
      </el-form>

      <el-divider>其他登录方式</el-divider>

      <!-- ========== 企微 OAuth 按钮（占位，Phase 3 接入） ========== -->
      <el-button
        type="success"
        size="large"
        :icon="ChatDotRound"
        class="wework-btn"
        disabled
        @click="handleWeWorkLogin"
      >
        企业微信登录（Phase 3 接入）
      </el-button>

      <div class="login-tip">
        <el-text type="info" size="small">
          <p>当前为 dev 模式：通过后端 <code>POST /api/auth/dev-login</code> 绕过企微 OAuth。</p>
          <p>企微登录将在 <strong>Phase 3</strong> 接入 corpId / agentId 后启用。</p>
        </el-text>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, ChatDotRound } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const form = reactive({ userId: '' })

const rules: FormRules = {
  userId: [
    { required: true, message: '请输入企微 UserID', trigger: 'blur' },
    { min: 1, max: 64, message: 'UserID 长度 1-64', trigger: 'blur' }
  ]
}

async function handleLogin() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    await userStore.loginDev(form.userId.trim())
    ElMessage.success(`欢迎回来，${userStore.userInfo?.name ?? form.userId}！`)
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (e) {
    // 错误已由 store.error 反映
  }
}

function handleWeWorkLogin() {
  ElMessage.info('企微登录将在 Phase 3 接入')
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  border-radius: 12px;
}

.login-header {
  text-align: center;
  padding: 8px 0 0 0;
}

.login-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.login-subtitle {
  margin: 6px 0 0 0;
  font-size: 12px;
  color: #909399;
  letter-spacing: 1px;
}

.login-btn {
  width: 100%;
}

.error-alert {
  margin-top: 8px;
}

.wework-btn {
  width: 100%;
  margin-bottom: 12px;
}

.login-tip {
  text-align: center;
  margin-top: 12px;
  line-height: 1.6;
}

.login-tip code {
  background-color: #f5f7fa;
  padding: 1px 5px;
  border-radius: 3px;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 12px;
  color: #d63384;
}

@media (max-width: 480px) {
  .login-container {
    padding: 12px;
  }
  .login-card {
    border-radius: 8px;
  }
}
</style>
