<template>
  <div class="calendar-container">
    <el-button :icon="ArrowLeft" @click="goBack" plain class="back-btn">返回设备列表</el-button>

    <el-card shadow="never" class="header-card" v-if="equipment">
      <div class="equipment-info">
        <div>
          <h2>{{ equipment.name }}</h2>
          <p class="meta">
            <el-tag size="small" effect="plain" class="meta-tag">{{ equipment.assetCode }}</el-tag>
            <el-tag size="small" :type="getStatusType(equipment.status) as any" class="meta-tag">
              {{ getStatusLabel(equipment.status) }}
            </el-tag>
            <span class="location">📍 {{ equipment.location || '位置未指定' }}</span>
          </p>
        </div>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog" :disabled="!canReserve">
          新建预约
        </el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="calendar-card">
      <template #header>
        <div class="card-header">
          <h3>预约日历</h3>
          <div class="date-nav">
            <el-date-picker
              v-model="monthRef"
              type="month"
              value-format="YYYY-MM"
              placeholder="选择月份"
              @change="onMonthChange"
            />
            <el-button text @click="prevMonth">‹ 上月</el-button>
            <el-button text @click="nextMonth">下月 ›</el-button>
          </div>
        </div>
      </template>

      <el-calendar v-model="calendarValue">
        <template #date-cell="{ data }">
          <div class="day-cell" @click="onDayClick(data.day)">
            <div
              v-for="r in getReservationsForDay(data.day)"
              :key="r.id"
              class="day-event"
              :class="`status-${r.status.toLowerCase()}`"
              :title="`${r.equipmentName || ''}\n${r.userName || r.userId}\n${r.startTime} ~ ${r.endTime}\n${r.purpose || ''}`"
            >
              {{ r.userName || r.userId }} · {{ formatTime(r.startTime) }} - {{ formatTime(r.endTime) }}
            </div>
          </div>
        </template>
      </el-calendar>
    </el-card>

    <!-- 某天的预约列表 -->
    <el-card v-if="selectedDay" shadow="never" class="day-list-card">
      <template #header>
        <div class="card-header">
          <h3>{{ selectedDay }} 的预约 ({{ dayReservations.length }})</h3>
          <el-button text @click="selectedDay = null">关闭</el-button>
        </div>
      </template>

      <el-empty v-if="dayReservations.length === 0" description="这一天没有预约" />

      <el-table v-else :data="dayReservations" border>
        <el-table-column label="时间" width="200">
          <template #default="{ row }">
            {{ formatTime(row.startTime) }} ~ {{ formatTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="userName" label="预约人" width="120">
          <template #default="{ row }">
            <span v-if="row.userName">{{ row.userName }}</span>
            <code v-else class="userid-code">{{ row.userId }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="purpose" label="用途" show-overflow-tooltip />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="getReservationStatusType(row.status) as any">
              {{ getReservationStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="canCancel(row)"
              size="small"
              type="danger"
              link
              @click="handleCancel(row)"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建预约对话框 -->
    <el-dialog v-model="dialogVisible" title="新建设备预约" width="520px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="使用目的" prop="purpose">
          <el-input v-model="form.purpose" type="textarea" :rows="2" placeholder="如：频谱测试、协议验证..." />
        </el-form-item>
        <el-form-item label="关联项目">
          <el-input v-model="form.projectId" placeholder="项目 ID（可选）" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.notes" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  getEquipment,
  listReservations,
  createReservation,
  cancelReservation,
  EQUIPMENT_STATUSES,
  type LabEquipmentVO,
  type EquipmentReservationVO
} from '@/api/equipment'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const equipmentId = computed(() => Number(route.query.equipmentId || 0))
const equipment = ref<LabEquipmentVO | null>(null)
const allReservations = ref<EquipmentReservationVO[]>([])
const calendarValue = ref(new Date())
const monthRef = ref<string>(
  `${new Date().getFullYear()}-${String(new Date().getMonth() + 1).padStart(2, '0')}`
)
const selectedDay = ref<string | null>(null)
const dayReservations = ref<EquipmentReservationVO[]>([])

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  startTime: '',
  endTime: '',
  purpose: '',
  projectId: undefined as number | undefined,
  notes: ''
})
const formRules: FormRules = {
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
}

const canReserve = computed(() => {
  if (!equipment.value) return false
  return ['AVAILABLE', 'IN_USE'].includes(equipment.value.status)
})

function getStatusLabel(s?: string) {
  return EQUIPMENT_STATUSES.find((x) => x.value === s)?.label ?? s ?? '-'
}
function getStatusType(s?: string) {
  return EQUIPMENT_STATUSES.find((x) => x.value === s)?.type ?? ''
}
function getReservationStatusLabel(s?: string) {
  return (
    {
      PENDING: '待确认',
      CONFIRMED: '已确认',
      IN_USE: '使用中',
      COMPLETED: '已完成',
      CANCELLED: '已取消',
      NO_SHOW: '未到场'
    }[s ?? ''] ?? s ?? '-'
  )
}
function getReservationStatusType(s?: string) {
  return (
    {
      PENDING: 'info',
      CONFIRMED: 'primary',
      IN_USE: 'warning',
      COMPLETED: 'success',
      CANCELLED: 'info',
      NO_SHOW: 'danger'
    }[s ?? ''] ?? ''
  )
}
function formatTime(s: string) {
  if (!s) return ''
  return s.length > 16 ? s.slice(11, 16) : s
}
function canCancel(row: EquipmentReservationVO): boolean {
  if (userStore.isAdmin) return true
  return row.userId === userStore.userInfo?.userId
    && !['IN_USE', 'COMPLETED', 'CANCELLED'].includes(row.status)
}

const getReservationsForDay = (day: string) => {
  return allReservations.value.filter((r) => {
    const start = r.startTime?.slice(0, 10)
    const end = r.endTime?.slice(0, 10)
    return day >= start && day <= end
  })
}

const onMonthChange = (val: string) => {
  if (val) {
    calendarValue.value = new Date(val + '-01')
    monthRef.value = val
  }
  loadReservations()
}
const prevMonth = () => {
  const [y, m] = monthRef.value.split('-').map(Number)
  const date = new Date(y, m - 2, 1)
  calendarValue.value = date
  monthRef.value = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
  loadReservations()
}
const nextMonth = () => {
  const [y, m] = monthRef.value.split('-').map(Number)
  const date = new Date(y, m, 1)
  calendarValue.value = date
  monthRef.value = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
  loadReservations()
}

const onDayClick = (day: string) => {
  selectedDay.value = day
  dayReservations.value = getReservationsForDay(day)
}

const loadEquipment = async () => {
  if (!equipmentId.value) {
    ElMessage.error('缺少设备 ID')
    router.push({ name: 'RdEquipment' })
    return
  }
  try {
    equipment.value = (await getEquipment(equipmentId.value)) as any
  } catch (e: any) {
    ElMessage.error('加载设备失败: ' + (e?.message || ''))
  }
}

const loadReservations = async () => {
  if (!equipmentId.value) return
  // 查整月 ± 1 周
  const [y, m] = monthRef.value.split('-').map(Number)
  const from = new Date(y, m - 1, -7).toISOString().slice(0, 19)
  const to = new Date(y, m, 7).toISOString().slice(0, 19)
  try {
    allReservations.value = (await listReservations(equipmentId.value, from, to)) as any
  } catch (e: any) {
    ElMessage.error('加载预约失败: ' + (e?.message || ''))
  }
}

const goBack = () => {
  router.push({ name: 'RdEquipment' })
}

const openCreateDialog = () => {
  Object.assign(form, {
    startTime: selectedDay.value ? `${selectedDay.value} 09:00:00` : '',
    endTime: selectedDay.value ? `${selectedDay.value} 10:00:00` : '',
    purpose: '',
    projectId: undefined,
    notes: ''
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

const handleSubmit = async () => {
  await formRef.value?.validate().catch(() => Promise.reject())
  submitting.value = true
  try {
    await createReservation(equipmentId.value, {
      startTime: form.startTime,
      endTime: form.endTime,
      purpose: form.purpose,
      projectId: form.projectId,
      notes: form.notes
    })
    ElMessage.success('预约已创建')
    dialogVisible.value = false
    await loadReservations()
    if (selectedDay.value) {
      dayReservations.value = getReservationsForDay(selectedDay.value)
    }
  } catch (e: any) {
    ElMessage.error('创建失败: ' + (e?.message || ''))
  } finally {
    submitting.value = false
  }
}

const handleCancel = async (row: EquipmentReservationVO) => {
  try {
    await ElMessageBox.confirm(
      `确定取消 ${formatTime(row.startTime)} - ${formatTime(row.endTime)} 的预约？`,
      '取消预约',
      { type: 'warning', confirmButtonText: '取消预约', cancelButtonText: '返回' }
    )
  } catch {
    return
  }
  try {
    await cancelReservation(row.id)
    ElMessage.success('已取消')
    await loadReservations()
    if (selectedDay.value) {
      dayReservations.value = getReservationsForDay(selectedDay.value)
    }
  } catch (e: any) {
    ElMessage.error('取消失败: ' + (e?.message || ''))
  }
}

onMounted(async () => {
  await loadEquipment()
  await loadReservations()
})
</script>

<style scoped>
.calendar-container {
  max-width: 1400px;
  margin: 0 auto;
}

.back-btn {
  margin-bottom: 12px;
}

.header-card,
.calendar-card,
.day-list-card {
  border-radius: 8px;
  margin-bottom: 16px;
}

.equipment-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.equipment-info h2 {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 600;
}

.meta {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.meta-tag {
  font-family: 'SFMono-Regular', Consolas, monospace;
}

.location {
  color: #909399;
  font-size: 13px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.date-nav {
  display: flex;
  align-items: center;
  gap: 8px;
}

.day-cell {
  min-height: 80px;
  padding: 2px;
  cursor: pointer;
}

.day-event {
  font-size: 11px;
  padding: 2px 4px;
  border-radius: 3px;
  margin-bottom: 2px;
  background-color: #ecf5ff;
  color: #409eff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.day-event.status-confirmed { background: #f0f9eb; color: #67c23a; }
.day-event.status-in_use    { background: #fdf6ec; color: #e6a23c; }
.day-event.status-pending   { background: #ecf5ff; color: #409eff; }
.day-event.status-completed { background: #f4f4f5; color: #909399; }
.day-event.status-cancelled { background: #fef0f0; color: #f56c6c; text-decoration: line-through; }

.userid-code {
  background: #f5f7fa;
  padding: 1px 6px;
  border-radius: 3px;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 12px;
  color: #d63384;
}

/* Element Plus 日历样式微调 */
:deep(.el-calendar) {
  --el-calendar-cell-width: 100px;
}

:deep(.el-calendar__body) {
  padding: 12px;
}

:deep(.el-calendar-day) {
  min-height: 90px;
  padding: 4px;
}
</style>
