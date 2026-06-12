<template>
  <div class="task-container">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" class="filter-form">
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 160px">
            <el-option v-for="s in TASK_STATUSES" :key="s" :label="statusLabel(s)" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="query.priority" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="p in TASK_PRIORITIES" :key="p" :label="priorityLabel(p)" :value="p" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="任务编号 / 标题"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          <el-button type="success" :icon="Plus" @click="handleCreate">新建任务</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table
        v-loading="loading"
        :data="taskList"
        stripe
        border
        :header-cell-style="{ background: '#f5f7fa', color: '#303133' }"
        empty-text="暂无任务"
      >
        <el-table-column prop="taskNo" label="任务编号" width="160" />
        <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
        <el-table-column label="优先级" width="100">
          <template #default="{ row }">
            <el-tag :type="priorityTagType(row.priority)" size="small">
              {{ priorityLabel(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发起人" width="110">
          <template #default="{ row }">
            {{ row.creatorName || row.creatorId }}
          </template>
        </el-table-column>
        <el-table-column label="接收人" width="110">
          <template #default="{ row }">
            {{ row.assigneeName || row.assigneeId }}
          </template>
        </el-table-column>
        <el-table-column label="截止时间" width="160">
          <template #default="{ row }">
            <span v-if="row.actualDeadline" :class="{ overdue: isOverdue(row) }">
              {{ formatDateTime(row.actualDeadline) }}
            </span>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleView(row)">查看</el-button>
            <!-- 状态流转按钮（按角色 + 状态动态显示） -->
            <template v-if="canAccept(row)">
              <el-button link type="success" size="small" @click="handleAccept(row)">接收</el-button>
            </template>
            <template v-if="canSubmit(row)">
              <el-button link type="primary" size="small" @click="handleSubmit(row)">提交</el-button>
            </template>
            <template v-if="canComplete(row)">
              <el-button link type="success" size="small" @click="handleComplete(row)">验收</el-button>
              <el-button link type="danger" size="small" @click="handleReject(row)">驳回</el-button>
            </template>
            <template v-if="canCancel(row)">
              <el-button link type="warning" size="small" @click="handleCancel(row)">撤回</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @current-change="fetchList"
        @size-change="fetchList"
      />
    </el-card>

    <!-- ========== 创建任务对话框 ========== -->
    <el-dialog
      v-model="createDialogVisible"
      title="新建任务"
      width="560px"
      :close-on-click-modal="false"
      @close="resetCreateForm"
    >
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-width="100px"
        label-position="right"
      >
        <el-form-item label="标题" prop="title">
          <el-input v-model="createForm.title" placeholder="简述任务内容" maxlength="256" show-word-limit />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            placeholder="任务详细说明（选填）"
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="执行人" prop="assigneeId">
          <el-input v-model="createForm.assigneeId" placeholder="企微 UserID（自指派填自己）" />
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="createForm.priority" placeholder="请选择" style="width: 100%">
            <el-option v-for="p in TASK_PRIORITIES" :key="p" :label="priorityLabel(p)" :value="p" />
          </el-select>
        </el-form-item>
        <el-form-item label="预估时长" prop="estimatedDuration">
          <el-input-number
            v-model="createForm.estimatedDuration"
            :min="1"
            :max="9999"
            placeholder="小时（选填）"
            controls-position="right"
          />
          <span class="form-tip">小时</span>
        </el-form-item>
        <el-form-item label="来源备注" prop="sourceRemark">
          <el-input v-model="createForm.sourceRemark" placeholder="如：企微需求单/邮件" maxlength="512" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreateSubmit">创建</el-button>
      </template>
    </el-dialog>

    <!-- ========== 任务详情对话框 ========== -->
    <el-dialog v-model="detailDialogVisible" title="任务详情" width="640px" :close-on-click-modal="false">
      <el-descriptions v-if="currentTask" :column="2" border>
        <el-descriptions-item label="任务编号">{{ currentTask.taskNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(currentTask.status)" size="small">
            {{ statusLabel(currentTask.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="优先级">
          <el-tag :type="priorityTagType(currentTask.priority)" size="small">
            {{ priorityLabel(currentTask.priority) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="发起人">
          {{ currentTask.creatorName || currentTask.creatorId }}
        </el-descriptions-item>
        <el-descriptions-item label="接收人">
          {{ currentTask.assigneeName || currentTask.assigneeId }}
        </el-descriptions-item>
        <el-descriptions-item label="预估时长">
          {{ currentTask.estimatedDuration ? currentTask.estimatedDuration + ' 小时' : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="开始时间" :span="2">
          {{ currentTask.actualStartTime ? formatDateTime(currentTask.actualStartTime) : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="截止时间" :span="2">
          <span v-if="currentTask.actualDeadline" :class="{ overdue: isOverdue(currentTask) }">
            {{ formatDateTime(currentTask.actualDeadline) }}
          </span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="完成时间" :span="2">
          {{ currentTask.actualEndTime ? formatDateTime(currentTask.actualEndTime) : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="驳回原因" :span="2" v-if="currentTask.rejectReason">
          {{ currentTask.rejectReason }}
        </el-descriptions-item>
        <el-descriptions-item label="标题" :span="2">{{ currentTask.title }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">
          <pre class="pre">{{ currentTask.description || '(无)' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(currentTask.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(currentTask.updatedAt) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- ========== 驳回原因对话框 ========== -->
    <el-dialog
      v-model="rejectDialogVisible"
      title="驳回任务"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form v-if="rejectTarget" label-width="80px">
        <el-form-item label="任务">
          <span class="task-ref">{{ rejectTarget.taskNo }} - {{ rejectTarget.title }}</span>
        </el-form-item>
        <el-form-item label="驳回原因" required>
          <el-input
            v-model="rejectReason"
            type="textarea"
            :rows="3"
            placeholder="请说明驳回原因，将记录到状态历史"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="actionLoading[rejectTarget?.id ?? -1]" @click="confirmReject">
          确认驳回
        </el-button>
      </template>
    </el-dialog>

    <!-- ========== 撤回原因对话框 ========== -->
    <el-dialog
      v-model="cancelDialogVisible"
      title="撤回任务"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form v-if="cancelTarget" label-width="80px">
        <el-form-item label="任务">
          <span class="task-ref">{{ cancelTarget.taskNo }} - {{ cancelTarget.title }}</span>
        </el-form-item>
        <el-form-item label="撤回原因">
          <el-input
            v-model="cancelReason"
            type="textarea"
            :rows="3"
            placeholder="选填：说明撤回原因，将记录到状态历史"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelDialogVisible = false">取消</el-button>
        <el-button type="warning" :loading="actionLoading[cancelTarget?.id ?? -1]" @click="confirmCancel">
          确认撤回
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import {
  listTasks,
  getTask,
  createTask,
  acceptTask,
  submitTask,
  completeTask,
  rejectTask,
  cancelTask,
  TASK_STATUSES,
  TASK_PRIORITIES,
  type TaskVO,
  type TaskQuery,
  type TaskStatus,
  type TaskPriority,
  type CreateTaskRequest
} from '@/api/task'
import { useUserStore } from '@/stores/user'

type ElTagType = 'primary' | 'success' | 'warning' | 'info' | 'danger' | ''

// ============== 列表 ==============

const loading = ref(false)
const taskList = ref<TaskVO[]>([])
const total = ref(0)

const query = reactive<TaskQuery>({
  status: undefined,
  priority: undefined,
  keyword: '',
  pageNum: 1,
  pageSize: 20
})

async function fetchList() {
  loading.value = true
  try {
    const res = await listTasks(query)
    taskList.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    // request.ts 拦截器已处理
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  fetchList()
}

function handleReset() {
  query.status = undefined
  query.priority = undefined
  query.keyword = ''
  query.pageNum = 1
  fetchList()
}

async function handleView(row: TaskVO) {
  try {
    const res = await getTask(row.id)
    currentTask.value = res.data
    detailDialogVisible.value = true
  } catch (e) {
    /* ignore */
  }
}

// ============== 标签映射 ==============

const STATUS_LABELS: Record<TaskStatus, string> = {
  PENDING_ACCEPT: '待接收',
  IN_PROGRESS: '进行中',
  PENDING_VERIFY: '待验收',
  COMPLETED: '已完成',
  REJECTED: '已驳回',
  WITHDRAWN: '已撤回'
}

const STATUS_TAG_TYPES: Record<TaskStatus, ElTagType> = {
  PENDING_ACCEPT: 'warning',
  IN_PROGRESS: 'primary',
  PENDING_VERIFY: 'warning',
  COMPLETED: 'success',
  REJECTED: 'danger',
  WITHDRAWN: 'info'
}

const PRIORITY_LABELS: Record<TaskPriority, string> = {
  1: 'P1 最高',
  2: 'P2 高',
  3: 'P3 中',
  4: 'P4 低'
}

const PRIORITY_TAG_TYPES: Record<TaskPriority, ElTagType> = {
  1: 'danger',
  2: 'warning',
  3: '',
  4: 'info'
}

function statusLabel(s: TaskStatus) { return STATUS_LABELS[s] ?? s }
function statusTagType(s: TaskStatus) { return STATUS_TAG_TYPES[s] ?? '' }
function priorityLabel(p: TaskPriority) { return PRIORITY_LABELS[p] ?? String(p) }
function priorityTagType(p: TaskPriority) { return PRIORITY_TAG_TYPES[p] ?? '' }

function formatDateTime(s: string) {
  if (!s) return '-'
  const d = new Date(s)
  if (isNaN(d.getTime())) return s
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function isOverdue(t: TaskVO): boolean {
  if (!t.actualDeadline) return false
  if (t.status === 'COMPLETED' || t.status === 'WITHDRAWN' || t.status === 'REJECTED') return false
  return new Date(t.actualDeadline).getTime() < Date.now()
}

// ============== 创建对话框 ==============

const createDialogVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive<CreateTaskRequest>({
  title: '',
  description: '',
  sourceRemark: '',
  assigneeId: '',
  priority: 3,
  estimatedDuration: undefined
})

const createRules: FormRules = {
  title: [{ required: true, message: '请输入任务标题', trigger: 'blur' }],
  assigneeId: [{ required: true, message: '请输入执行人 UserID', trigger: 'blur' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }]
}

function handleCreate() {
  createDialogVisible.value = true
}

function resetCreateForm() {
  createForm.title = ''
  createForm.description = ''
  createForm.sourceRemark = ''
  createForm.assigneeId = ''
  createForm.priority = 3
  createForm.estimatedDuration = undefined
  createFormRef.value?.clearValidate()
}

async function handleCreateSubmit() {
  if (!createFormRef.value) return
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  createLoading.value = true
  try {
    const res = await createTask({
      title: createForm.title,
      description: createForm.description || undefined,
      sourceRemark: createForm.sourceRemark || undefined,
      assigneeId: createForm.assigneeId,
      priority: createForm.priority,
      estimatedDuration: createForm.estimatedDuration
    })
    ElMessage.success(`任务已创建：${res.data.taskNo}`)
    createDialogVisible.value = false
    handleSearch()
  } catch (e) {
    /* ignore */
  } finally {
    createLoading.value = false
  }
}

// ============== 详情对话框 ==============

const detailDialogVisible = ref(false)
const currentTask = ref<TaskVO | null>(null)

// ============== 初始化 ==============

onMounted(() => {
  fetchList()
})

// ============== 状态流转（按角色 + 状态动态显示） ==============

const userStore = useUserStore()

/**
 * 通用操作 loading 状态（key: taskId）
 */
const actionLoading = reactive<Record<number, boolean>>({})

function setActionLoading(id: number, on: boolean) {
  actionLoading[id] = on
}

// === 操作权限判定 ===

function isCreator(row: TaskVO) {
  return row.creatorId === userStore.userInfo?.userId
}
function isAssignee(row: TaskVO) {
  return row.assigneeId === userStore.userInfo?.userId
}

function canAccept(row: TaskVO) {
  return row.status === 'PENDING_ACCEPT' && isAssignee(row)
}
function canSubmit(row: TaskVO) {
  return row.status === 'IN_PROGRESS' && isAssignee(row)
}
function canComplete(row: TaskVO) {
  return row.status === 'PENDING_VERIFY' && isCreator(row)
}
function canCancel(row: TaskVO) {
  return (row.status === 'PENDING_ACCEPT' || row.status === 'IN_PROGRESS') && isCreator(row)
}

// === 操作 handler ===

async function handleAccept(row: TaskVO) {
  await runAction(row, () => acceptTask(row.id), '已接收')
}

async function handleSubmit(row: TaskVO) {
  // submit 可选 remark，简化为直接调（前端可后续加 prompt）
  await runAction(row, () => submitTask(row.id), '已提交待验收')
}

async function handleComplete(row: TaskVO) {
  try {
    await ElMessageBox.confirm(
      `确认验收任务「${row.title}」？此操作不可撤销。`,
      '验收确认',
      { type: 'success', confirmButtonText: '验收', cancelButtonText: '取消' }
    )
  } catch { return }
  await runAction(row, () => completeTask(row.id), '已验收')
}

function handleReject(row: TaskVO) {
  rejectTarget.value = row
  rejectReason.value = ''
  rejectDialogVisible.value = true
}

function handleCancel(row: TaskVO) {
  cancelTarget.value = row
  cancelReason.value = ''
  cancelDialogVisible.value = true
}

async function confirmReject() {
  const row = rejectTarget.value
  if (!row) return
  const reason = rejectReason.value.trim()
  if (!reason) {
    ElMessage.warning('请输入驳回原因')
    return
  }
  await runAction(row, () => rejectTask(row.id, reason), '已驳回，接收方可继续处理')
  rejectDialogVisible.value = false
}

async function confirmCancel() {
  const row = cancelTarget.value
  if (!row) return
  const reason = cancelReason.value.trim() || undefined
  await runAction(row, () => cancelTask(row.id, reason), '已撤回任务')
  cancelDialogVisible.value = false
}

/**
 * 通用 action 包装：loading + 调接口 + 提示 + 刷新
 */
async function runAction(row: TaskVO, fn: () => Promise<unknown>, successMsg: string) {
  setActionLoading(row.id, true)
  try {
    await fn()
    ElMessage.success(successMsg)
    handleSearch()
  } catch (e) {
    /* request.ts 拦截器已 toast */
  } finally {
    setActionLoading(row.id, false)
  }
}

// === 驳回 / 撤回原因对话框 ===

const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const rejectTarget = ref<TaskVO | null>(null)

const cancelDialogVisible = ref(false)
const cancelReason = ref('')
const cancelTarget = ref<TaskVO | null>(null)
</script>

<style scoped>
.task-container {
  padding: 0;
}
.filter-card,
.table-card {
  border-radius: 6px;
  margin-bottom: 16px;
}
.filter-form {
  margin-bottom: -16px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
.muted {
  color: #c0c4cc;
}
.overdue {
  color: #f56c6c;
  font-weight: 600;
}
.form-tip {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
.pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  color: #606266;
}
.task-ref {
  color: #606266;
  font-size: 13px;
}
</style>
