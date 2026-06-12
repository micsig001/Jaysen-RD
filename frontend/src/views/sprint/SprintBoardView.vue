<template>
  <div class="sprint-board">
    <!-- ========== 顶部工具栏 ========== -->
    <el-card shadow="never" class="toolbar">
      <div class="toolbar-row">
        <el-select
          v-model="filterProjectId"
          placeholder="选择项目"
          clearable
          style="width: 220px"
          @change="handleProjectChange"
        >
          <el-option
            v-for="p in projectOptions"
            :key="p.id"
            :label="`${p.code} - ${p.name}`"
            :value="p.id"
          />
        </el-select>

        <el-select
          v-model="activeSprintId"
          placeholder="选择 Sprint"
          clearable
          style="width: 260px"
          :disabled="!filterProjectId"
          @change="handleSprintChange"
        >
          <el-option
            v-for="s in sprintOptions"
            :key="s.id"
            :label="`${s.name} (${s.status})`"
            :value="s.id"
          />
        </el-select>

        <el-button type="primary" :icon="Plus" :disabled="!filterProjectId" @click="handleCreateSprint">
          新建 Sprint
        </el-button>
        <el-button
          v-if="activeSprint && activeSprint.status === 'PLANNED'"
          type="success"
          :icon="VideoPlay"
          :loading="actionLoading.activate"
          @click="handleActivate"
        >
          启动 Sprint
        </el-button>
        <el-button
          v-if="activeSprint && activeSprint.status === 'ACTIVE'"
          type="warning"
          :icon="CircleCheck"
          :loading="actionLoading.complete"
          @click="handleComplete"
        >
          完成 Sprint
        </el-button>
        <el-button
          v-if="activeSprint"
          type="success"
          :icon="DocumentAdd"
          @click="handleCreateTask"
        >
          新建任务
        </el-button>

        <div class="spacer" />

        <span v-if="activeSprint" class="sprint-info">
          <el-tag :type="sprintStatusTagType(activeSprint.status)" size="small">
            {{ sprintStatusLabel(activeSprint.status) }}
          </el-tag>
          <span class="muted">
            {{ activeSprint.startDate }} ~ {{ activeSprint.endDate }}
          </span>
          <span class="muted">
            任务 {{ activeSprint.taskCount }} / 完成 {{ activeSprint.doneCount }}
          </span>
        </span>
      </div>
    </el-card>

    <!-- ========== 看板 5 列 ========== -->
    <div v-if="!activeSprint" class="empty-tip">
      <el-empty description="请选择项目 + Sprint 查看看板" :image-size="100" />
    </div>
    <div v-else v-loading="loading" class="board-container">
      <div class="board">
        <div
          v-for="col in columns"
          :key="col.status"
          class="board-column"
        >
          <!-- 列头 -->
          <div class="column-header" :style="{ background: col.color }">
            <span class="column-title">{{ col.label }}</span>
            <el-tag size="small" effect="dark">{{ getColumnTasks(col.status).length }}</el-tag>
          </div>
          <!-- 列体（拖拽区） -->
          <draggable
            v-model="columnTasksMap[col.status]"
            :animation="200"
            group="sprint-tasks"
            item-key="id"
            class="column-body"
            ghost-class="drag-ghost"
            chosen-class="drag-chosen"
            @end="(e: any) => handleDragEnd(col.status, e)"
          >
            <template #item="{ element: t }">
              <div class="task-card" :class="`priority-${t.priority.toLowerCase()}`" @click="handleTaskClick(t)">
                <div class="task-card-header">
                  <el-tag :type="typeTagType(t.type)" size="small" effect="plain">
                    {{ typeLabel(t.type) }}
                  </el-tag>
                  <el-tag :type="priorityTagType(t.priority)" size="small">
                    {{ priorityLabel(t.priority) }}
                  </el-tag>
                </div>
                <div class="task-title">{{ t.title }}</div>
                <div v-if="t.storyPoints != null" class="task-points">
                  <el-icon><Trophy /></el-icon>
                  <span>{{ t.storyPoints }} pts</span>
                </div>
                <div class="task-footer">
                  <span v-if="t.assigneeName" class="assignee">
                    <el-avatar :size="20" :src="undefined">
                      {{ t.assigneeName.charAt(0) }}
                    </el-avatar>
                    <span>{{ t.assigneeName }}</span>
                  </span>
                  <span v-else class="muted">未分配</span>
                  <span v-if="t.dueDate" class="muted">{{ t.dueDate }}</span>
                </div>
              </div>
            </template>
          </draggable>
        </div>
      </div>
    </div>

    <!-- ========== 新建 Sprint 对话框 ========== -->
    <el-dialog v-model="sprintDialogVisible" title="新建 Sprint" width="480px">
      <el-form ref="sprintFormRef" :model="sprintForm" :rules="sprintRules" label-width="100px">
        <el-form-item label="项目" prop="projectId">
          <el-select v-model="sprintForm.projectId" placeholder="选择项目" style="width: 100%">
            <el-option v-for="p in projectOptions" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="sprintForm.name" placeholder="如: Sprint 12 - EVT 主板" maxlength="256" />
        </el-form-item>
        <el-form-item label="起止日期" prop="dateRange">
          <el-date-picker
            v-model="sprintForm.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="目标">
          <el-input v-model="sprintForm.goal" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sprintDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="sprintCreating" @click="handleCreateSprintSubmit">创建</el-button>
      </template>
    </el-dialog>

    <!-- ========== 新建任务对话框 ========== -->
    <el-dialog v-model="taskDialogVisible" title="新建 Sprint 任务" width="520px">
      <el-form ref="taskFormRef" :model="taskForm" :rules="taskRules" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="taskForm.title" maxlength="512" show-word-limit />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="taskForm.type" style="width: 100%">
            <el-option v-for="t in SPRINT_TASK_TYPES" :key="t" :label="typeLabel(t)" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="taskForm.priority" style="width: 100%">
            <el-option v-for="p in SPRINT_TASK_PRIORITIES" :key="p" :label="priorityLabel(p)" :value="p" />
          </el-select>
        </el-form-item>
        <el-form-item label="接收人" prop="assigneeUserid">
          <el-input v-model="taskForm.assigneeUserid" placeholder="企微 UserID" />
        </el-form-item>
        <el-form-item label="故事点" prop="storyPoints">
          <el-input-number v-model="taskForm.storyPoints" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="taskForm.dueDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="taskForm.description" type="textarea" :rows="3" maxlength="1000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="taskCreating" @click="handleCreateTaskSubmit">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  Plus,
  VideoPlay,
  CircleCheck,
  DocumentAdd,
  Trophy
} from '@element-plus/icons-vue'
import { VueDraggable as draggable } from 'vue-draggable-plus'
import {
  listSprints,
  createSprint,
  activateSprint,
  completeSprint,
  listSprintTasks,
  createSprintTask,
  moveSprintTask,
  SPRINT_TASK_STATUSES,
  SPRINT_TASK_TYPES,
  SPRINT_TASK_PRIORITIES,
  type SprintVO,
  type SprintTaskVO,
  type SprintTaskStatus,
  type SprintTaskType,
  type SprintTaskPriority,
  type CreateSprintTaskRequest
} from '@/api/sprint'
import { listProjects, type ProjectVO } from '@/api/project'

type ElTagType = 'primary' | 'success' | 'warning' | 'info' | 'danger' | ''

// ========== 数据 ==========

const loading = ref(false)
const sprintOptions = ref<SprintVO[]>([])
const projectOptions = ref<ProjectVO[]>([])
const filterProjectId = ref<number | undefined>()
const activeSprintId = ref<number | undefined>()
const activeSprint = computed(() => sprintOptions.value.find(s => s.id === activeSprintId.value))

// 5 列看板数据, key = status, value = tasks 数组
type StatusMap = Record<SprintTaskStatus, SprintTaskVO[]>
const columnTasksMap = reactive<StatusMap>({
  BACKLOG: [],
  TODO: [],
  IN_PROGRESS: [],
  REVIEW: [],
  DONE: []
})

// 看板列定义
interface ColumnDef { status: SprintTaskStatus; label: string; color: string }
const columns: ColumnDef[] = [
  { status: 'BACKLOG', label: '待规划', color: '#909399' },
  { status: 'TODO', label: '待办', color: '#409eff' },
  { status: 'IN_PROGRESS', label: '进行中', color: '#e6a23c' },
  { status: 'REVIEW', label: '待验收', color: '#67c23a' },
  { status: 'DONE', label: '已完成', color: '#c0c4cc' }
]

function getColumnTasks(status: SprintTaskStatus): SprintTaskVO[] {
  return columnTasksMap[status] || []
}

// ========== 加载数据 ==========

async function fetchProjects() {
  try {
    const res = await listProjects({ pageSize: 200 })
    projectOptions.value = res.data.records
  } catch (e) {
    projectOptions.value = []
  }
}

async function fetchSprints() {
  if (!filterProjectId.value) {
    sprintOptions.value = []
    return
  }
  try {
    sprintOptions.value = (await listSprints({ projectId: filterProjectId.value })).data
    if (sprintOptions.value.length > 0 && !activeSprintId.value) {
      // 默认选第一个 ACTIVE 的,否则选第一个
      const active = sprintOptions.value.find(s => s.status === 'ACTIVE')
      activeSprintId.value = active ? active.id : sprintOptions.value[0].id
    }
    if (activeSprintId.value) {
      await fetchTasks()
    }
  } catch (e) {
    sprintOptions.value = []
  }
}

async function fetchTasks() {
  if (!activeSprintId.value) {
    Object.keys(columnTasksMap).forEach(k => {
      columnTasksMap[k as SprintTaskStatus] = []
    })
    return
  }
  loading.value = true
  try {
    const res = await listSprintTasks({ sprintId: activeSprintId.value })
    const all = res.data
    // 按 status 分组
    Object.keys(columnTasksMap).forEach(k => {
      columnTasksMap[k as SprintTaskStatus] = []
    })
    for (const t of all) {
      if (columnTasksMap[t.status]) {
        columnTasksMap[t.status].push(t)
      }
    }
  } catch (e) {
    /* ignore */
  } finally {
    loading.value = false
  }
}

function handleProjectChange() {
  activeSprintId.value = undefined
  fetchSprints()
}

function handleSprintChange() {
  fetchTasks()
}

// ========== 操作按钮 ==========

const actionLoading = reactive({ activate: false, complete: false })

async function handleActivate() {
  if (!activeSprint.value) return
  actionLoading.activate = true
  try {
    await activateSprint(activeSprint.value.id)
    ElMessage.success('Sprint 已启动')
    await fetchSprints()
  } catch (e) { /* ignore */ } finally { actionLoading.activate = false }
}

async function handleComplete() {
  if (!activeSprint.value) return
  try {
    await ElMessageBox.confirm(
      `确认完成 Sprint「${activeSprint.value.name}」? 进行中的任务不会被自动关闭。`,
      '完成 Sprint',
      { type: 'warning', confirmButtonText: '完成', cancelButtonText: '取消' }
    )
  } catch { return }
  actionLoading.complete = true
  try {
    await completeSprint(activeSprint.value.id)
    ElMessage.success('Sprint 已完成')
    await fetchSprints()
  } catch (e) { /* ignore */ } finally { actionLoading.complete = false }
}

// ========== 拖拽 ==========

/**
 * draggable @end 事件: 拿到 from/to 容器 index, 重新算 order_num, 调 move 接口
 */
async function handleDragEnd(toStatus: SprintTaskStatus, _event: any) {
  // 找出本次拖动的任务
  const targetList = columnTasksMap[toStatus]
  // draggable v-model 已经更新了 targetList 顺序, 我们取最后位置(刚拖入的)
  const movedTask = targetList[targetList.length - 1]
  if (!movedTask) return
  // 重算 order_num: 该列内索引
  const newOrderNum = targetList.length - 1
  try {
    await moveSprintTask(movedTask.id, {
      sprintId: movedTask.sprintId ?? null,
      status: toStatus,
      orderNum: newOrderNum
    })
    ElMessage.success(`已移动到「${columns.find(c => c.status === toStatus)?.label}」`)
  } catch (e) {
    // 失败回滚: 重新拉数据
    await fetchTasks()
  }
}

// ========== Sprint 创建对话框 ==========

const sprintDialogVisible = ref(false)
const sprintCreating = ref(false)
const sprintFormRef = ref<FormInstance>()
const sprintForm = reactive({
  projectId: undefined as number | undefined,
  name: '',
  dateRange: [] as string[],
  goal: ''
})
const sprintRules: FormRules = {
  projectId: [{ required: true, message: '请选择项目', trigger: 'change' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  dateRange: [{ required: true, message: '请选择起止日期', trigger: 'change' }]
}

function handleCreateSprint() {
  sprintForm.projectId = filterProjectId.value
  sprintForm.name = ''
  sprintForm.dateRange = []
  sprintForm.goal = ''
  sprintDialogVisible.value = true
}

async function handleCreateSprintSubmit() {
  if (!sprintFormRef.value) return
  const valid = await sprintFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (sprintForm.dateRange.length !== 2) return
  sprintCreating.value = true
  try {
    const res = await createSprint({
      projectId: sprintForm.projectId!,
      name: sprintForm.name,
      startDate: sprintForm.dateRange[0],
      endDate: sprintForm.dateRange[1],
      goal: sprintForm.goal || undefined
    })
    ElMessage.success(`Sprint「${res.data.name}」已创建`)
    sprintDialogVisible.value = false
    await fetchSprints()
  } catch (e) { /* ignore */ } finally { sprintCreating.value = false }
}

// ========== 任务创建对话框 ==========

const taskDialogVisible = ref(false)
const taskCreating = ref(false)
const taskFormRef = ref<FormInstance>()
const taskForm = reactive<CreateSprintTaskRequest & { dateRange?: string[] }>({
  projectId: 0,
  sprintId: undefined,
  title: '',
  description: '',
  type: 'FEATURE',
  priority: 'MEDIUM',
  assigneeUserid: '',
  storyPoints: 1,
  dueDate: undefined
})
const taskRules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }]
}

function handleCreateTask() {
  taskForm.projectId = filterProjectId.value!
  taskForm.sprintId = activeSprintId.value
  taskForm.title = ''
  taskForm.description = ''
  taskForm.type = 'FEATURE'
  taskForm.priority = 'MEDIUM'
  taskForm.assigneeUserid = ''
  taskForm.storyPoints = 1
  taskForm.dueDate = undefined
  taskDialogVisible.value = true
}

async function handleCreateTaskSubmit() {
  if (!taskFormRef.value) return
  const valid = await taskFormRef.value.validate().catch(() => false)
  if (!valid) return
  taskCreating.value = true
  try {
    await createSprintTask({
      projectId: taskForm.projectId,
      sprintId: taskForm.sprintId,
      title: taskForm.title,
      description: taskForm.description || undefined,
      type: taskForm.type,
      priority: taskForm.priority,
      assigneeUserid: taskForm.assigneeUserid || undefined,
      storyPoints: taskForm.storyPoints,
      dueDate: taskForm.dueDate
    })
    ElMessage.success('任务已创建')
    taskDialogVisible.value = false
    await fetchTasks()
  } catch (e) { /* ignore */ } finally { taskCreating.value = false }
}

function handleTaskClick(t: SprintTaskVO) {
  ElMessage.info(`任务: ${t.title}（详情编辑暂未实现，可拖拽改列）`)
}

// ========== 标签映射 ==========

const SPRINT_STATUS_LABELS: Record<string, string> = {
  PLANNED: '已规划', ACTIVE: '进行中', COMPLETED: '已完成', CANCELLED: '已取消'
}
const SPRINT_STATUS_TAG_TYPES: Record<string, ElTagType> = {
  PLANNED: 'info', ACTIVE: 'success', COMPLETED: '', CANCELLED: 'danger'
}
const TYPE_LABELS: Record<SprintTaskType, string> = {
  FEATURE: '功能', BUG: '缺陷', OPTIMIZATION: '优化', TEST: '测试'
}
const TYPE_TAG_TYPES: Record<SprintTaskType, ElTagType> = {
  FEATURE: 'primary', BUG: 'danger', OPTIMIZATION: 'warning', TEST: 'info'
}
const PRIORITY_LABELS: Record<SprintTaskPriority, string> = {
  LOW: '低', MEDIUM: '中', HIGH: '高', CRITICAL: '紧急'
}
const PRIORITY_TAG_TYPES: Record<SprintTaskPriority, ElTagType> = {
  LOW: 'info', MEDIUM: '', HIGH: 'warning', CRITICAL: 'danger'
}

function sprintStatusLabel(s: string) { return SPRINT_STATUS_LABELS[s] ?? s }
function sprintStatusTagType(s: string) { return SPRINT_STATUS_TAG_TYPES[s] ?? '' }
function typeLabel(t: SprintTaskType) { return TYPE_LABELS[t] ?? t }
function typeTagType(t: SprintTaskType) { return TYPE_TAG_TYPES[t] ?? '' }
function priorityLabel(p: SprintTaskPriority) { return PRIORITY_LABELS[p] ?? p }
function priorityTagType(p: SprintTaskPriority) { return PRIORITY_TAG_TYPES[p] ?? '' }

onMounted(() => {
  fetchProjects()
})
</script>

<style scoped>
.sprint-board {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
}
.toolbar {
  border-radius: 6px;
  margin-bottom: 12px;
  flex-shrink: 0;
}
.toolbar-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.spacer { flex: 1; }
.sprint-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}
.muted {
  color: #909399;
}
.empty-tip {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
.board-container {
  flex: 1;
  overflow-x: auto;
  overflow-y: hidden;
}
.board {
  display: flex;
  gap: 12px;
  height: 100%;
  min-width: max-content;
}
.board-column {
  flex: 0 0 280px;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  border-radius: 6px;
  overflow: hidden;
}
.column-header {
  padding: 10px 12px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  flex-shrink: 0;
}
.column-title {
  font-size: 14px;
}
.column-body {
  flex: 1;
  padding: 8px;
  overflow-y: auto;
  min-height: 100px;
}
.task-card {
  background: #fff;
  border-radius: 4px;
  padding: 10px;
  margin-bottom: 8px;
  cursor: pointer;
  border-left: 3px solid #dcdfe6;
  transition: box-shadow 0.2s, transform 0.1s;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}
.task-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  transform: translateY(-1px);
}
.task-card.priority-critical {
  border-left-color: #f56c6c;
}
.task-card.priority-high {
  border-left-color: #e6a23c;
}
.task-card.priority-medium {
  border-left-color: #909399;
}
.task-card.priority-low {
  border-left-color: #c0c4cc;
}
.task-card-header {
  display: flex;
  gap: 4px;
  margin-bottom: 6px;
}
.task-title {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  line-height: 1.4;
  margin-bottom: 6px;
  word-break: break-word;
}
.task-points {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #e6a23c;
  margin-bottom: 4px;
}
.task-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #606266;
}
.assignee {
  display: flex;
  align-items: center;
  gap: 4px;
}
.drag-ghost {
  opacity: 0.4;
  background: #e6a23c !important;
}
.drag-chosen {
  cursor: grabbing;
}
</style>
