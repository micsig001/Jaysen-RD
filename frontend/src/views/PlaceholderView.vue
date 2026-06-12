<template>
  <div class="placeholder-container">
    <el-card shadow="hover" class="placeholder-card">
      <div class="placeholder-content">
        <el-icon class="placeholder-icon"><DocumentRemove /></el-icon>

        <h2 class="placeholder-title">{{ title }}</h2>
        <p class="placeholder-subtitle">{{ subtitle }}</p>

        <el-divider />

        <el-descriptions :column="1" border class="meta-table">
          <el-descriptions-item label="路由路径">
            <code>{{ routePath }}</code>
          </el-descriptions-item>
          <el-descriptions-item label="页面标题">
            {{ title }}
          </el-descriptions-item>
          <el-descriptions-item v-if="module" label="所属模块">
            <el-tag :type="module === 'rd' ? 'warning' : 'primary'" size="small">
              {{ module === 'rd' ? '研发管理' : '任务管理' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="phase" label="计划阶段">
            <el-tag type="info" size="small">{{ phase }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="requiresAdmin" label="权限要求">
            <el-tag type="danger" size="small">仅 ADMIN</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-alert
          type="info"
          :closable="false"
          show-icon
          class="phase-tip"
        >
          <template #title>
            这是 Phase 1 骨架占位页面 —— 业务模块将在后续 Phase 实现
          </template>
        </el-alert>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { DocumentRemove } from '@element-plus/icons-vue'

const route = useRoute()

const routePath = computed(() => route.path)
const title = computed(() => (route.meta?.title as string) || '研发管理')
const module = computed(() => route.meta?.module as string | undefined)
const phase = computed(() => route.meta?.phase as string | undefined)
const requiresAdmin = computed(() => Boolean(route.meta?.requiresAdmin))

const subtitle = computed(() => {
  if (module.value === 'rd') {
    return '研发管理模块占位页面 — 由 Phase 2 业务实现替换'
  }
  if (module.value === 'task') {
    return '任务管理模块占位页面 — 由 Task 项目代码搬入后替换'
  }
  return '研发管理系统占位页面'
})
</script>

<style scoped>
.placeholder-container {
  max-width: 720px;
  margin: 40px auto;
  padding: 0 20px;
}

.placeholder-card {
  border-radius: 8px;
}

.placeholder-content {
  text-align: center;
  padding: 24px 8px;
}

.placeholder-icon {
  font-size: 64px;
  color: #c0c4cc;
  margin-bottom: 16px;
}

.placeholder-title {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.placeholder-subtitle {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.meta-table {
  margin: 24px 0;
  text-align: left;
}

.meta-table code {
  background-color: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  color: #d63384;
}

.phase-tip {
  margin-top: 16px;
  text-align: left;
}
</style>
