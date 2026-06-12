<template>
  <div class="hello-container">
    <el-container>
      <el-header class="page-header">
        <h1>仪器类产品研发管理系统</h1>
        <p class="subtitle">Instrument R&amp;D Management System · Phase 1 骨架</p>
      </el-header>

      <el-main>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-card shadow="hover" class="info-card">
              <template #header>
                <div class="card-header">
                  <el-icon><Cpu /></el-icon>
                  <span>技术栈</span>
                </div>
              </template>
              <ul class="info-list">
                <li>后端：Spring Boot 3.2 + MyBatis-Plus + Flowable 7.1.0</li>
                <li>前端：Vue 3 + TypeScript + Element Plus + ECharts</li>
                <li>数据库：MySQL 8.0 + Flyway</li>
                <li>缓存：Redis 7</li>
              </ul>
            </el-card>
          </el-col>

          <el-col :span="8">
            <el-card shadow="hover" class="info-card">
              <template #header>
                <div class="card-header">
                  <el-icon><Calendar /></el-icon>
                  <span>Phase 1 已交付</span>
                </div>
              </template>
              <ul class="info-list">
                <li>✅ 项目骨架（后端 + 前端 + Docker Compose）</li>
                <li>✅ V1 全量 SQL（25 张表）</li>
                <li>✅ 根包 com.RD 全配置</li>
                <li>⏳ Phase 2-4：项目/设备/ECN/看板/BOM/缺陷</li>
              </ul>
            </el-card>
          </el-col>

          <el-col :span="8">
            <el-card shadow="hover" class="info-card">
              <template #header>
                <div class="card-header">
                  <el-icon><Connection /></el-icon>
                  <span>系统状态</span>
                </div>
              </template>
              <el-descriptions :column="1" border>
                <el-descriptions-item label="根包">com.RD</el-descriptions-item>
                <el-descriptions-item label="路径">E:\Project\RD</el-descriptions-item>
                <el-descriptions-item label="后端端口">8080</el-descriptions-item>
                <el-descriptions-item label="前端端口">5173</el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-col>
        </el-row>

        <el-alert
          class="top-tip"
          type="info"
          :closable="false"
          show-icon
        >
          <template #title>
            这只是 Phase 1 骨架页面 —— 业务模块（项目管理 / 设备预约 / ECN 审批）将在后续 Phase 实现
          </template>
        </el-alert>
      </el-main>

      <el-footer class="page-footer">
        <span>Apache 2.0 License · Mavis · {{ year }}</span>
      </el-footer>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { Cpu, Calendar, Connection } from '@element-plus/icons-vue'

const year = new Date().getFullYear()
</script>

<style scoped>
.hello-container {
  min-height: 100vh;
}

.page-header {
  background: linear-gradient(135deg, #409eff 0%, #36cfc9 100%);
  color: white;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  padding: 0;
  height: 140px;
}

.page-header h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 600;
}

.subtitle {
  margin: 8px 0 0 0;
  font-size: 14px;
  opacity: 0.9;
}

.info-card {
  border-radius: 8px;
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.info-list {
  margin: 0;
  padding-left: 20px;
  line-height: 1.8;
  color: #606266;
}

.top-tip {
  margin-top: 20px;
}

.page-footer {
  text-align: center;
  color: #909399;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
