<template>
  <div class="dashboard-container">
    <!-- ========== 顶部 4 块大数字 ========== -->
    <el-row :gutter="12" class="kpi-row">
      <el-col :xs="12" :sm="6" :md="6" v-for="kpi in kpiCards" :key="kpi.label">
        <el-card shadow="hover" class="kpi-card" :body-style="{ padding: '20px' }">
          <div class="kpi-icon" :style="{ background: kpi.color }">
            <el-icon :size="28"><component :is="kpi.icon" /></el-icon>
          </div>
          <div class="kpi-body">
            <div class="kpi-value">{{ kpi.value }}</div>
            <div class="kpi-label">{{ kpi.label }}</div>
            <div v-if="kpi.subLabel" class="kpi-sub">{{ kpi.subLabel }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ========== 7 日趋势 + 任务状态/优先级 ========== -->
    <el-row :gutter="12" class="chart-row">
      <el-col :xs="24" :lg="16">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-header">
              <span class="chart-title">7 日趋势</span>
              <span class="chart-sub">任务 / 缺陷 新增与完成</span>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-canvas" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-header">
              <span class="chart-title">任务状态分布</span>
              <span class="chart-sub">6 态统计</span>
            </div>
          </template>
          <div ref="taskStatusChartRef" class="chart-canvas" />
        </el-card>
      </el-col>
    </el-row>

    <!-- ========== 任务优先级 + 逾期 Top 10 ========== -->
    <el-row :gutter="12" class="chart-row">
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-header">
              <span class="chart-title">任务优先级分布</span>
              <span class="chart-sub">高/中/低/闲</span>
            </div>
          </template>
          <div ref="taskPriorityChartRef" class="chart-canvas" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="16">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-header">
              <span class="chart-title">当前逾期任务 Top 10</span>
              <span class="chart-sub">共 {{ taskDashboard.overdueCount || 0 }} 条逾期</span>
            </div>
          </template>
          <el-table
            :data="taskDashboard.overdueTop || []"
            stripe
            size="small"
            empty-text="暂无逾期任务"
            max-height="320"
          >
            <el-table-column prop="taskNo" label="编号" width="150" />
            <el-table-column label="标题" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">
                <el-link type="primary" :underline="false" @click="goTask(row.id)">
                  {{ row.title }}
                </el-link>
              </template>
            </el-table-column>
            <el-table-column label="处理人" width="100">
              <template #default="{ row }">
                {{ row.assigneeName || row.assigneeUserid || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="逾期" width="100">
              <template #default="{ row }">
                <el-tag type="danger" size="small">{{ row.overdueDays }} 天</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- ========== 设备状态 + 类别 ========== -->
    <el-row :gutter="12" class="chart-row">
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-header">
              <span class="chart-title">设备状态分布</span>
              <span class="chart-sub">总 {{ overview.equipmentCount || 0 }} 台</span>
            </div>
          </template>
          <div ref="equipmentStatusChartRef" class="chart-canvas" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-header">
              <span class="chart-title">设备类别分布</span>
              <span class="chart-sub">按 V1 SQL 6 类</span>
            </div>
          </template>
          <div ref="equipmentCategoryChartRef" class="chart-canvas" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-header">
              <span class="chart-title">即将校准</span>
              <span class="chart-sub">Top 10</span>
            </div>
          </template>
          <div class="cal-list">
            <div
              v-for="c in equipmentDashboard.calibrationDue || []"
              :key="c.equipmentId"
              class="cal-item"
            >
              <span class="cal-name" :title="c.equipmentName">
                {{ c.equipmentName }}
              </span>
              <el-tag
                :type="c.daysUntilDue < 0 ? 'danger' : c.daysUntilDue < 7 ? 'warning' : 'info'"
                size="small"
              >
                {{ c.daysUntilDue < 0 ? `已过期 ${-c.daysUntilDue} 天` : `${c.daysUntilDue} 天后` }}
              </el-tag>
            </div>
            <div v-if="!(equipmentDashboard.calibrationDue || []).length" class="empty">
              暂无即将到期校准
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ========== 设备利用率 Top 5 (7 日预约) ========== -->
    <el-row :gutter="12" class="chart-row">
      <el-col :span="24">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-header">
              <span class="chart-title">设备利用率 Top 5 (7 日预约次数)</span>
              <span class="chart-sub">{{ equipmentDashboard.recent7Days?.join(' / ') }}</span>
            </div>
          </template>
          <div ref="equipmentUsageChartRef" class="chart-canvas" style="height: 360px" />
        </el-card>
      </el-col>
    </el-row>

    <!-- ========== 活跃 Sprint 进度 ========== -->
    <el-row :gutter="12" class="chart-row">
      <el-col :span="24">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-header">
              <span class="chart-title">活跃 Sprint 进度</span>
              <span class="chart-sub">共 {{ sprintDashboard.activeSprintCount || 0 }} 个</span>
            </div>
          </template>
          <el-table
            :data="sprintDashboard.activeSprints || []"
            stripe
            empty-text="暂无活跃 Sprint"
          >
            <el-table-column label="Sprint" min-width="220">
              <template #default="{ row }">
                <el-link type="primary" :underline="false" @click="goSprint(row.sprintId)">
                  {{ row.sprintName }}
                </el-link>
                <div class="sub-text">{{ row.projectName || '-' }}</div>
              </template>
            </el-table-column>
            <el-table-column label="时间" width="220">
              <template #default="{ row }">
                {{ row.startDate }} ~ {{ row.endDate }}
              </template>
            </el-table-column>
            <el-table-column label="任务" width="120">
              <template #default="{ row }">
                {{ row.doneTasks }} / {{ row.totalTasks }}
              </template>
            </el-table-column>
            <el-table-column label="故事点" width="120">
              <template #default="{ row }">
                {{ row.doneStoryPoints }} / {{ row.totalStoryPoints }}
              </template>
            </el-table-column>
            <el-table-column label="进度" min-width="280">
              <template #default="{ row }">
                <el-progress
                  :percentage="row.progressPercent"
                  :status="row.progressPercent >= 100 ? 'success' : ''"
                />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- ========== 处理人负载 Top 10 ========== -->
    <el-row :gutter="12" class="chart-row">
      <el-col :span="24">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-header">
              <span class="chart-title">处理人负载 Top 10 (按进行中任务数)</span>
              <span class="chart-sub">数据来源 task.assignee_id</span>
            </div>
          </template>
          <el-table
            :data="taskDashboard.assigneeTop || []"
            stripe
            size="small"
            empty-text="暂无处理人"
            max-height="380"
          >
            <el-table-column label="处理人" min-width="160">
              <template #default="{ row }">
                {{ row.assigneeName || row.assigneeUserid }}
              </template>
            </el-table-column>
            <el-table-column label="UserID" prop="assigneeUserid" width="160" />
            <el-table-column label="进行中任务" width="140" sortable>
              <template #default="{ row }">
                <el-tag size="small">{{ row.openCount }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="其中逾期" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.overdueCount > 0" type="danger" size="small">
                  {{ row.overdueCount }}
                </el-tag>
                <span v-else class="muted">-</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts/core'
import {
  BarChart,
  LineChart,
  PieChart
} from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import {
  Folder,
  List,
  Warning,
  Histogram,
  Box
} from '@element-plus/icons-vue'
import {
  getOverview,
  getTaskDashboard,
  getEquipmentDashboard,
  getSprintDashboard,
  EQUIPMENT_STATUS_COLORS,
  EQUIPMENT_STATUS_LABELS,
  EQUIPMENT_CATEGORY_LABELS,
  TASK_STATUS_COLORS,
  TASK_STATUS_LABELS,
  TASK_PRIORITY_COLORS,
  TASK_PRIORITY_LABELS,
  type DashboardOverviewVO,
  type TaskDashboardVO,
  type EquipmentDashboardVO,
  type SprintDashboardVO
} from '@/api/dashboard'

// ECharts 组件注册
echarts.use([
  BarChart, LineChart, PieChart,
  TitleComponent, TooltipComponent, LegendComponent, GridComponent,
  CanvasRenderer
])

const router = useRouter()

// ============== 数据 ==============

const overview = reactive<DashboardOverviewVO>({
  projectCount: 0, activeProjectCount: 0,
  taskCount: 0, openTaskCount: 0,
  defectCount: 0, openDefectCount: 0,
  activeSprintCount: 0, equipmentCount: 0, availableEquipmentCount: 0,
  recent7Days: [], taskCreatedTrend: {}, taskCompletedTrend: {},
  defectCreatedTrend: {}, defectClosedTrend: {}
})
const taskDashboard = reactive<TaskDashboardVO>({
  statusDistribution: {}, priorityDistribution: {}, typeDistribution: {},
  recent7Days: [], createdTrend: {}, completedTrend: {},
  overdueCount: 0, overdueTop: [], assigneeTop: []
})
const equipmentDashboard = reactive<EquipmentDashboardVO>({
  statusDistribution: {}, categoryDistribution: {},
  recent7Days: [], usage: [], calibrationDue: [], topUsage: []
})
const sprintDashboard = reactive<SprintDashboardVO>({
  activeSprintCount: 0, activeSprints: []
})

// ============== 顶部 KPI ==============

const kpiCards = computed(() => [
  {
    label: '项目', subLabel: `${overview.activeProjectCount || 0} 个进行中`,
    value: overview.projectCount || 0, color: '#409eff', icon: Folder
  },
  {
    label: '任务', subLabel: `${overview.openTaskCount || 0} 个未完成`,
    value: overview.taskCount || 0, color: '#67c23a', icon: List
  },
  {
    label: '缺陷', subLabel: `${overview.openDefectCount || 0} 个未关闭`,
    value: overview.defectCount || 0, color: '#f56c6c', icon: Warning
  },
  {
    label: '活跃 Sprint', subLabel: `${overview.availableEquipmentCount || 0} / ${overview.equipmentCount || 0} 设备可用`,
    value: overview.activeSprintCount || 0, color: '#e6a23c', icon: Histogram
  }
])

// ============== Chart refs ==============

const trendChartRef = ref<HTMLDivElement>()
const taskStatusChartRef = ref<HTMLDivElement>()
const taskPriorityChartRef = ref<HTMLDivElement>()
const equipmentStatusChartRef = ref<HTMLDivElement>()
const equipmentCategoryChartRef = ref<HTMLDivElement>()
const equipmentUsageChartRef = ref<HTMLDivElement>()

let trendChart: echarts.ECharts | null = null
let taskStatusChart: echarts.ECharts | null = null
let taskPriorityChart: echarts.ECharts | null = null
let equipmentStatusChart: echarts.ECharts | null = null
let equipmentCategoryChart: echarts.ECharts | null = null
let equipmentUsageChart: echarts.ECharts | null = null

// ============== 数据加载 ==============

async function loadAll() {
  try {
    const [ov, td, ed, sd] = await Promise.all([
      getOverview(),
      getTaskDashboard(),
      getEquipmentDashboard(),
      getSprintDashboard()
    ])
    Object.assign(overview, ov.data)
    Object.assign(taskDashboard, td.data)
    Object.assign(equipmentDashboard, ed.data)
    Object.assign(sprintDashboard, sd.data)
    await nextTick()
    renderAllCharts()
  } catch (e: any) {
    // 失败也不刷, 让用户看到空状态
    console.error('[Dashboard] 加载失败', e)
  }
}

function goTask(id: number) {
  router.push(`/tasks`).then(() => {
    // TODO 后续可加 query.taskId 高亮
  })
}

function goSprint(id: number) {
  router.push(`/rd/board`)
}

// ============== ECharts 渲染 ==============

function renderAllCharts() {
  renderTrendChart()
  renderTaskStatusChart()
  renderTaskPriorityChart()
  renderEquipmentStatusChart()
  renderEquipmentCategoryChart()
  renderEquipmentUsageChart()
}

function renderTrendChart() {
  if (!trendChartRef.value) return
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)
  const days = overview.recent7Days || []
  const dateLabels = days.map(d => d.slice(5))  // MM-DD
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['任务新增', '任务完成', '缺陷新增', '缺陷关闭'], top: 0 },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: dateLabels, axisLine: { lineStyle: { color: '#ddd' } } },
    yAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed' } } },
    series: [
      {
        name: '任务新增', type: 'line', smooth: true, symbol: 'circle', symbolSize: 6,
        lineStyle: { color: '#409eff', width: 2 },
        itemStyle: { color: '#409eff' },
        data: days.map(d => overview.taskCreatedTrend?.[d] || 0)
      },
      {
        name: '任务完成', type: 'line', smooth: true, symbol: 'circle', symbolSize: 6,
        lineStyle: { color: '#67c23a', width: 2 },
        itemStyle: { color: '#67c23a' },
        data: days.map(d => overview.taskCompletedTrend?.[d] || 0)
      },
      {
        name: '缺陷新增', type: 'line', smooth: true, symbol: 'circle', symbolSize: 6,
        lineStyle: { color: '#f56c6c', width: 2, type: 'dashed' },
        itemStyle: { color: '#f56c6c' },
        data: days.map(d => overview.defectCreatedTrend?.[d] || 0)
      },
      {
        name: '缺陷关闭', type: 'line', smooth: true, symbol: 'circle', symbolSize: 6,
        lineStyle: { color: '#e6a23c', width: 2, type: 'dashed' },
        itemStyle: { color: '#e6a23c' },
        data: days.map(d => overview.defectClosedTrend?.[d] || 0)
      }
    ]
  })
}

function renderTaskStatusChart() {
  if (!taskStatusChartRef.value) return
  if (!taskStatusChart) taskStatusChart = echarts.init(taskStatusChartRef.value)
  const dist = taskDashboard.statusDistribution || {}
  const data = Object.keys(dist).map(k => ({
    name: TASK_STATUS_LABELS[k] || k,
    value: dist[k],
    itemStyle: { color: TASK_STATUS_COLORS[k] || '#909399' }
  }))
  taskStatusChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 0, top: 'middle', textStyle: { fontSize: 12 } },
    series: [{
      type: 'pie', radius: ['45%', '70%'], center: ['65%', '50%'],
      avoidLabelOverlap: true,
      label: { show: false }, labelLine: { show: false },
      data
    }]
  })
}

function renderTaskPriorityChart() {
  if (!taskPriorityChartRef.value) return
  if (!taskPriorityChart) taskPriorityChart = echarts.init(taskPriorityChartRef.value)
  const dist = taskDashboard.priorityDistribution || {}
  const order = ['1', '2', '3', '4']
  taskPriorityChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 80, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed' } } },
    yAxis: {
      type: 'category',
      data: order.map(k => TASK_PRIORITY_LABELS[k] || k),
      axisLine: { lineStyle: { color: '#ddd' } }
    },
    series: [{
      type: 'bar', barWidth: '50%',
      data: order.map(k => ({
        value: dist[k] || 0,
        itemStyle: { color: TASK_PRIORITY_COLORS[k] || '#909399' }
      })),
      label: { show: true, position: 'right' }
    }]
  })
}

function renderEquipmentStatusChart() {
  if (!equipmentStatusChartRef.value) return
  if (!equipmentStatusChart) equipmentStatusChart = echarts.init(equipmentStatusChartRef.value)
  const dist = equipmentDashboard.statusDistribution || {}
  const data = Object.keys(dist).map(k => ({
    name: EQUIPMENT_STATUS_LABELS[k] || k,
    value: dist[k],
    itemStyle: { color: EQUIPMENT_STATUS_COLORS[k] || '#909399' }
  }))
  equipmentStatusChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, textStyle: { fontSize: 12 } },
    series: [{
      type: 'pie', radius: '60%', center: ['50%', '45%'],
      label: { formatter: '{b}\n{c}' },
      data
    }]
  })
}

function renderEquipmentCategoryChart() {
  if (!equipmentCategoryChartRef.value) return
  if (!equipmentCategoryChart) equipmentCategoryChart = echarts.init(equipmentCategoryChartRef.value)
  const dist = equipmentDashboard.categoryDistribution || {}
  const data = Object.keys(dist).map(k => ({
    name: EQUIPMENT_CATEGORY_LABELS[k] || k,
    value: dist[k]
  }))
  equipmentCategoryChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, textStyle: { fontSize: 12 } },
    series: [{
      type: 'pie', radius: '60%', center: ['50%', '45%'],
      label: { formatter: '{b}\n{c}' },
      data
    }]
  })
}

function renderEquipmentUsageChart() {
  if (!equipmentUsageChartRef.value) return
  if (!equipmentUsageChart) equipmentUsageChart = echarts.init(equipmentUsageChartRef.value)
  const top = equipmentDashboard.topUsage || []
  const days = (equipmentDashboard.recent7Days || []).map(d => d.slice(5))
  equipmentUsageChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: days, top: 0, type: 'scroll' },
    grid: { left: 40, right: 20, top: 50, bottom: 30 },
    xAxis: { type: 'category', data: top.map(e => e.equipmentName) },
    yAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed' } } },
    series: days.map((d, i) => ({
      name: d, type: 'bar', stack: 'usage', barWidth: '60%',
      itemStyle: { color: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452'][i] },
      data: top.map(e => e.dailyCounts?.[i] || 0)
    }))
  })
}

// ============== Resize ==============

function handleResize() {
  trendChart?.resize()
  taskStatusChart?.resize()
  taskPriorityChart?.resize()
  equipmentStatusChart?.resize()
  equipmentCategoryChart?.resize()
  equipmentUsageChart?.resize()
}

// ============== 生命周期 ==============

onMounted(() => {
  loadAll()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  taskStatusChart?.dispose()
  taskPriorityChart?.dispose()
  equipmentStatusChart?.dispose()
  equipmentCategoryChart?.dispose()
  equipmentUsageChart?.dispose()
})
</script>

<style scoped>
.dashboard-container {
  padding: 16px;
}

.kpi-row {
  margin-bottom: 12px;
}

.kpi-card {
  display: flex;
  align-items: center;
}

.kpi-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  width: 100%;
}

.kpi-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  margin-right: 16px;
}

.kpi-body {
  flex: 1;
  min-width: 0;
}

.kpi-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  line-height: 1.2;
}

.kpi-label {
  font-size: 13px;
  color: #303133;
  margin-top: 4px;
}

.kpi-sub {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.chart-row {
  margin-bottom: 12px;
}

.chart-card {
  height: 100%;
}

.chart-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.chart-title {
  font-weight: 600;
  color: #303133;
  font-size: 15px;
}

.chart-sub {
  font-size: 12px;
  color: #909399;
}

.chart-canvas {
  height: 280px;
  width: 100%;
}

.cal-list {
  max-height: 320px;
  overflow-y: auto;
}

.cal-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 4px;
  border-bottom: 1px solid #f5f5f5;
}

.cal-item:last-child {
  border-bottom: none;
}

.cal-name {
  font-size: 13px;
  color: #303133;
  flex: 1;
  margin-right: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.empty {
  text-align: center;
  color: #c0c4cc;
  padding: 32px 0;
  font-size: 13px;
}

.muted {
  color: #c0c4cc;
}

.sub-text {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
</style>
