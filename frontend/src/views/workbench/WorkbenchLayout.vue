<template>
  <el-container class="layout-container">
    <!-- ============== 侧边栏（PC 端固定） ============== -->
    <el-aside :width="isMobile ? '100%' : '240px'" v-if="!isMobile">
      <div class="sidebar-header">
        <h3>仪器研发管理</h3>
        <p class="sidebar-subtitle">Instrument R&amp;D</p>
      </div>

      <el-scrollbar class="sidebar-scrollbar">
        <!-- ========== 分组 1：任务管理（合并自 Task 项目） ========== -->
        <el-menu
          :default-active="activeMenu"
          router
          class="sidebar-menu"
        >
          <el-menu-item-group title="任务管理">
            <el-menu-item index="/tasks">
              <el-icon><List /></el-icon>
              <span>我的任务</span>
            </el-menu-item>
            <el-menu-item index="/tasks/legacy-history">
              <el-icon><Clock /></el-icon>
              <span>历史记录</span>
            </el-menu-item>
            <el-menu-item index="/tasks/legacy-viz">
              <el-icon><Connection /></el-icon>
              <span>关系图谱</span>
            </el-menu-item>
            <el-menu-item index="/tasks/legacy-admin" v-if="userStore.isAdmin">
              <el-icon><Setting /></el-icon>
              <span>管理后台</span>
            </el-menu-item>
          </el-menu-item-group>

          <!-- ========== 分组 2：研发管理（Phase 1 占位，Phase 2 填真实路由） ========== -->
          <el-menu-item-group title="研发管理">
            <el-menu-item index="/rd/projects">
              <el-icon><Folder /></el-icon>
              <span>项目管理</span>
              <el-tag size="small" type="info" class="phase-tag">P2</el-tag>
            </el-menu-item>
            <el-menu-item index="/rd/milestones">
              <el-icon><DataLine /></el-icon>
              <span>里程碑甘特图</span>
              <el-tag size="small" type="info" class="phase-tag">P2</el-tag>
            </el-menu-item>
            <el-menu-item index="/rd/equipment">
              <el-icon><Cpu /></el-icon>
              <span>实验室设备</span>
              <el-tag size="small" type="info" class="phase-tag">P2</el-tag>
            </el-menu-item>
            <el-menu-item index="/rd/board">
              <el-icon><Tickets /></el-icon>
              <span>敏捷看板</span>
              <el-tag size="small" type="info" class="phase-tag">P2</el-tag>
            </el-menu-item>
            <el-menu-item index="/rd/bom">
              <el-icon><Files /></el-icon>
              <span>BOM 管理</span>
              <el-tag size="small" type="info" class="phase-tag">P3</el-tag>
            </el-menu-item>
            <el-menu-item index="/rd/ecn">
              <el-icon><Document /></el-icon>
              <span>工程变更 ECN</span>
              <el-tag size="small" type="info" class="phase-tag">P2</el-tag>
            </el-menu-item>
            <el-menu-item index="/rd/defects">
              <el-icon><Warning /></el-icon>
              <span>缺陷跟踪</span>
              <el-tag size="small" type="info" class="phase-tag">P3</el-tag>
            </el-menu-item>
            <el-menu-item index="/rd/dashboard">
              <el-icon><PieChart /></el-icon>
              <span>研发仪表盘</span>
              <el-tag size="small" type="info" class="phase-tag">P3</el-tag>
            </el-menu-item>
          </el-menu-item-group>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <!-- ============== 主区域 ============== -->
    <el-container>
      <!-- 顶栏 -->
      <el-header class="layout-header">
        <div class="header-left">
          <el-button
            v-if="isMobile"
            :icon="showMobileMenu ? 'Fold' : 'Expand'"
            circle
            @click="showMobileMenu = !showMobileMenu"
          />
          <span class="page-title">{{ currentPageTitle }}</span>
        </div>
        <div class="header-right">
          <el-tag
            v-if="userStore.userInfo"
            :type="roleTagType"
            size="small"
            class="role-tag"
          >
            {{ roleLabel }}
          </el-tag>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :src="userStore.userInfo?.avatar" />
              <span class="username">{{ userStore.userInfo?.name || '未登录' }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 移动端抽屉菜单 -->
      <el-drawer
        v-model="showMobileMenu"
        title="菜单"
        direction="ltr"
        size="70%"
        v-if="isMobile"
      >
        <el-menu
          :default-active="activeMenu"
          router
          @select="showMobileMenu = false"
        >
          <el-menu-item-group title="任务管理">
            <el-menu-item index="/tasks">
              <el-icon><List /></el-icon>
              <span>我的任务</span>
            </el-menu-item>
            <el-menu-item index="/tasks/legacy-history">
              <el-icon><Clock /></el-icon>
              <span>历史记录</span>
            </el-menu-item>
          </el-menu-item-group>
          <el-menu-item-group title="研发管理">
            <el-menu-item index="/rd/projects">
              <el-icon><Folder /></el-icon>
              <span>项目管理</span>
            </el-menu-item>
            <el-menu-item index="/rd/equipment">
              <el-icon><Cpu /></el-icon>
              <span>实验室设备</span>
            </el-menu-item>
            <el-menu-item index="/rd/board">
              <el-icon><Tickets /></el-icon>
              <span>敏捷看板</span>
            </el-menu-item>
            <el-menu-item index="/rd/ecn">
              <el-icon><Document /></el-icon>
              <span>工程变更 ECN</span>
            </el-menu-item>
          </el-menu-item-group>
        </el-menu>
      </el-drawer>

      <!-- 内容区 -->
      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  List,
  Clock,
  Connection,
  Setting,
  Folder,
  DataLine,
  Cpu,
  Tickets,
  Files,
  Document,
  Warning,
  PieChart
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

// 路由 + Store
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 响应式
const isMobile = ref(window.innerWidth < 768)
const showMobileMenu = ref(false)

// 当前路径用于高亮菜单
const activeMenu = computed(() => route.path)

// 页面标题（从路由 meta.title 取，fallback 到"研发管理"）
const currentPageTitle = computed(() => {
  return (route.meta?.title as string) || '研发管理'
})

// 角色显示
const ROLE_LABELS: Record<string, string> = {
  EMPLOYEE: '普通员工',
  MANAGER: '部门经理',
  ADMIN: '超级管理员'
}
const ROLE_TAG_TYPES: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
  EMPLOYEE: 'info',
  MANAGER: 'warning',
  ADMIN: 'danger'
}
const roleLabel = computed(() => {
  const role = userStore.userInfo?.role
  return role ? (ROLE_LABELS[role] ?? role) : '访客'
})
const roleTagType = computed(() => {
  const role = userStore.userInfo?.role
  return role ? (ROLE_TAG_TYPES[role] ?? '') : 'info'
})

// 响应窗口尺寸
const handleResize = () => {
  isMobile.value = window.innerWidth < 768
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

// 顶栏下拉
const handleCommand = async (command: string) => {
  if (command === 'profile') {
    ElMessage.info('个人中心 — Phase 2 接入')
    return
  }
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch {
      return
    }
    // Phase 2 接入：const { removeToken } = await import('@/utils/request')
    userStore.clearUserInfo()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.el-aside {
  background: linear-gradient(180deg, #304156 0%, #263445 100%);
  color: #fff;
  transition: width 0.3s;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  height: 64px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  flex-shrink: 0;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 17px;
  color: #fff;
  font-weight: 600;
}

.sidebar-subtitle {
  margin: 2px 0 0 0;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 1px;
}

.sidebar-scrollbar {
  flex: 1;
}

.sidebar-menu {
  border-right: none;
  background-color: transparent;
}

/* 分组标题 */
:deep(.el-menu-item-group__title) {
  color: rgba(255, 255, 255, 0.4) !important;
  font-size: 12px;
  letter-spacing: 1px;
  padding-left: 16px !important;
}

:deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.75);
  height: 44px;
  line-height: 44px;
}

:deep(.el-menu-item:hover) {
  background-color: rgba(255, 255, 255, 0.05) !important;
  color: #fff;
}

:deep(.el-menu-item.is-active) {
  background-color: rgba(64, 158, 255, 0.2) !important;
  color: #fff;
  border-right: 3px solid #409eff;
}

/* Phase 标签（菜单项右侧小标签） */
.phase-tag {
  margin-left: auto;
  margin-right: 8px;
  height: 18px;
  line-height: 17px;
  padding: 0 4px;
  font-size: 10px;
}

/* 顶栏 */
.layout-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 20px;
  height: 56px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.role-tag {
  font-weight: 500;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.user-info:hover {
  background-color: #f5f7fa;
}

.username {
  font-size: 14px;
  color: #303133;
}

/* 主内容区 */
.layout-main {
  background-color: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}

/* 路由切换淡入 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 移动端 */
@media (max-width: 768px) {
  .layout-main {
    padding: 10px;
  }

  .username {
    display: none;
  }

  .role-tag {
    display: none;
  }
}
</style>
