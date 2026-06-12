import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/request'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Workbench',
      component: () => import('@/views/workbench/WorkbenchLayout.vue'),
      redirect: '/tasks',
      children: [
        // ============== 任务管理（合并自 Task 项目，Phase 2 接入真实接口） ==============
        {
          path: 'tasks',
          name: 'TaskList',
          component: () => import('@/views/task/TaskListView.vue'),
          meta: { title: '我的任务', module: 'task' }
        },
        {
          path: 'tasks/legacy-history',
          name: 'TaskHistory',
          component: () => import('@/views/PlaceholderView.vue'),
          meta: { title: '历史记录', module: 'task' }
        },
        {
          path: 'tasks/legacy-viz',
          name: 'TaskVisualization',
          component: () => import('@/views/PlaceholderView.vue'),
          meta: { title: '关系图谱', module: 'task' }
        },
        {
          path: 'tasks/legacy-admin',
          name: 'TaskAdmin',
          component: () => import('@/views/PlaceholderView.vue'),
          meta: { title: '管理后台', module: 'task', requiresAdmin: true }
        },

        // ============== 研发管理（Phase 2 接入真实页面） ==============
        {
          path: 'rd/projects',
          name: 'RdProjects',
          component: () => import('@/views/rd/ProjectListView.vue'),
          meta: { title: '项目管理', module: 'rd', phase: 'P2' }
        },
        {
          path: 'rd/projects/:id',
          name: 'RdProjectDetail',
          component: () => import('@/views/rd/ProjectDetailView.vue'),
          meta: { title: '项目详情', module: 'rd', phase: 'P2' }
        },
        {
          path: 'rd/milestones',
          name: 'RdMilestones',
          component: () => import('@/views/PlaceholderView.vue'),
          meta: { title: '里程碑甘特图', module: 'rd', phase: 'P3' }
        },
        {
          path: 'rd/equipment',
          name: 'RdEquipment',
          component: () => import('@/views/rd/EquipmentListView.vue'),
          meta: { title: '实验室设备', module: 'rd', phase: 'P2' }
        },
        {
          path: 'rd/equipment/calendar',
          name: 'RdEquipmentCalendar',
          component: () => import('@/views/rd/EquipmentCalendarView.vue'),
          meta: { title: '设备预约日历', module: 'rd', phase: 'P2' }
        },
        {
          path: 'rd/board',
          name: 'RdBoard',
          component: () => import('@/views/sprint/SprintBoardView.vue'),
          meta: { title: '敏捷看板', module: 'rd', phase: 'P2' }
        },
        {
          path: 'rd/bom',
          name: 'RdBom',
          component: () => import('@/views/PlaceholderView.vue'),
          meta: { title: 'BOM 管理', module: 'rd', phase: 'P3' }
        },
        {
          path: 'rd/ecn',
          name: 'RdEcn',
          component: () => import('@/views/ecn/EcnListView.vue'),
          meta: { title: '工程变更 ECN', module: 'rd', phase: 'P2' }
        },
        {
          path: 'rd/ecn/:id',
          name: 'RdEcnDetail',
          component: () => import('@/views/ecn/EcnDetailView.vue'),
          meta: { title: 'ECN 详情', module: 'rd', phase: 'P2' }
        },
        {
          path: 'rd/defects',
          name: 'RdDefects',
          component: () => import('@/views/PlaceholderView.vue'),
          meta: { title: '缺陷跟踪', module: 'rd', phase: 'P3' }
        },
        {
          path: 'rd/dashboard',
          name: 'RdDashboard',
          component: () => import('@/views/PlaceholderView.vue'),
          meta: { title: '研发仪表盘', module: 'rd', phase: 'P3' }
        }
      ]
    },

    // ============== 登录（dev 模式直登，企微 OAuth Phase 3 接入） ==============
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginView.vue'),
      meta: { title: '登录', isPublic: true }
    },

    // ============== 企微 OAuth 回调接收页（从 URL 拿 token 存 localStorage） ==============
    {
      path: '/login/callback',
      name: 'LoginCallback',
      component: () => import('@/views/auth/LoginCallbackView.vue'),
      meta: { title: '登录中...', isPublic: true }
    },

    // ============== 403 无权访问 ==============
    {
      path: '/403',
      name: 'Forbidden',
      component: () => import('@/views/PlaceholderView.vue'),
      meta: { title: '无权访问', isPublic: true }
    },

    // ============== 404 ==============
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/PlaceholderView.vue'),
      meta: { title: '页面未找到', isPublic: true }
    }
  ]
})

/**
 * 路由守卫
 *
 * <p>职责：</p>
 * <ol>
 *   <li>公开页面（isPublic）直接放行</li>
 *   <li>未登录访问受保护页面 → 重定向到 /login</li>
 *   <li>已登录访问 /login → 重定向到 /</li>
 *   <li>需要 ADMIN 角色但当前用户不是 → 重定向到 /403</li>
 *   <li>PC 专属页面（pcOnly）移动端访问 → 重定向到 /mobile-restricted</li>
 * </ol>
 */
router.beforeEach((to, _from, next) => {
  // 1) 公开页面直接放行
  if (to.meta?.isPublic) {
    next()
    return
  }

  // 2) 检查 Token
  const token = getToken()
  if (!token) {
    next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
    return
  }

  // 3) 已登录访问 /login → 重定向到首页
  if (to.path === '/login') {
    next('/')
    return
  }

  // 4) ADMIN 角色检查
  if (to.meta?.requiresAdmin) {
    const userStore = useUserStore()
    if (!userStore.isAdmin) {
      next('/403')
      return
    }
  }

  // 5) PC 端限制（768px 断点）
  if (to.meta?.pcOnly && window.innerWidth < 768) {
    next('/mobile-restricted')
    return
  }

  next()
})

export default router
