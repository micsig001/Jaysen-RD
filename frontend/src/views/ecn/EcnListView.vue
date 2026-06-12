<template>
  <div class="ecn-container">
    <!-- ========== 待办横幅 (有 pending 时显示) ========== -->
    <el-alert
      v-if="pendingTasks.length > 0"
      :title="`您有 ${pendingTasks.length} 个待审批的 ECN`"
      type="warning"
      :closable="false"
      show-icon
      class="pending-banner"
    >
      <template #default>
        <div class="pending-list">
          <el-tag
            v-for="t in pendingTasks"
            :key="t.id"
            type="warning"
            class="pending-tag"
            @click="goDetail(t.id)"
          >
            {{ t.ecnNumber }} - {{ t.title }}
          </el-tag>
        </div>
      </template>
    </el-alert>

    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" class="filter-form">
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 160px">
            <el-option v-for="s in ECN_STATUSES" :key="s" :label="statusLabel(s)" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.changeType" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="t in ECN_CHANGE_TYPES" :key="t" :label="changeTypeLabel(t)" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="紧急度">
          <el-select v-model="query.urgency" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="u in ECN_URGENCIES" :key="u" :label="urgencyLabel(u)" :value="u" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="ECN 编号 / 标题"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          <el-button type="success" :icon="Plus" @click="handleCreate">新建 ECN</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table
        v-loading="loading"
        :data="ecnList"
        stripe
        border
        :header-cell-style="{ background: '#f5f7fa', color: '#303133' }"
        empty-text="暂无 ECN"
        @row-click="goDetail"
      >
        <el-table-column prop="ecnNumber" label="ECN 编号" width="170" />
        <el-table-column prop="title" label="标题" min-width="280" show-overflow-tooltip />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag size="small">{{ changeTypeLabel(row.changeType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="紧急度" width="100">
          <template #default="{ row }">
            <el-tag :type="urgencyTagType(row.urgency)" size="small">
              {{ urgencyLabel(row.urgency) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发起人" width="110">
          <template #default="{ row }">
            {{ row.requesterName || row.requesterUserid }}
          </template>
        </el-table-column>
        <el-table-column label="目标日期" width="120">
          <template #default="{ row }">
            {{ row.targetDate || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
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

    <!-- ========== 创建 ECN 对话框 ========== -->
    <el-dialog
      v-model="createDialogVisible"
      title="新建 ECN"
      width="640px"
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
          <el-input v-model="createForm.title" placeholder="简述 ECN 主题" maxlength="512" show-word-limit />
        </el-form-item>
        <el-form-item label="变更类型" prop="changeType">
          <el-select v-model="createForm.changeType" placeholder="请选择" style="width: 100%">
            <el-option v-for="t in ECN_CHANGE_TYPES" :key="t" :label="changeTypeLabel(t)" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="紧急度" prop="urgency">
          <el-select v-model="createForm.urgency" placeholder="默认 NORMAL" style="width: 100%">
            <el-option v-for="u in ECN_URGENCIES" :key="u" :label="urgencyLabel(u)" :value="u" />
          </el-select>
        </el-form-item>
        <el-form-item label="变更原因" prop="reason">
          <el-input
            v-model="createForm.reason"
            type="textarea"
            :rows="2"
            placeholder="为什么需要此次变更？"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="变更描述" prop="description">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            placeholder="详细描述变更内容（涉及哪些模块/物料/工艺）"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="影响分析" prop="impactAnalysis">
          <el-input
            v-model="createForm.impactAnalysis"
            type="textarea"
            :rows="2"
            placeholder="评估对成本/进度/质量的影响（选填）"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="目标日期" prop="targetDate">
          <el-date-picker
            v-model="createForm.targetDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="计划完成日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="createForm.priority" :min="0" :max="100" />
          <span class="form-tip">0-100，数字越大越优先</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreateSubmit">创建草稿</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import {
  listEcn,
  createEcn,
  listMyPendingEcnTasks,
  ECN_STATUSES,
  ECN_CHANGE_TYPES,
  ECN_URGENCIES,
  type EcnChangeVO,
  type EcnStatus,
  type EcnChangeType,
  type EcnUrgency,
  type EcnQuery,
  type CreateEcnRequest
} from '@/api/ecn'

type ElTagType = 'primary' | 'success' | 'warning' | 'info' | 'danger' | ''

const router = useRouter()

// ========== 列表 ==========

const loading = ref(false)
const ecnList = ref<EcnChangeVO[]>([])
const total = ref(0)
const pendingTasks = ref<EcnChangeVO[]>([])

const query = reactive<EcnQuery>({
  status: undefined,
  changeType: undefined,
  urgency: undefined,
  keyword: '',
  pageNum: 1,
  pageSize: 20
})

async function fetchList() {
  loading.value = true
  try {
    const res = await listEcn(query)
    ecnList.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    /* request.ts 拦截器已处理 */
  } finally {
    loading.value = false
  }
}

async function fetchPending() {
  try {
    pendingTasks.value = (await listMyPendingEcnTasks()).data
  } catch (e) {
    pendingTasks.value = []
  }
}

function handleSearch() {
  query.pageNum = 1
  fetchList()
}

function handleReset() {
  query.status = undefined
  query.changeType = undefined
  query.urgency = undefined
  query.keyword = ''
  query.pageNum = 1
  fetchList()
}

function goDetail(id: number) {
  router.push({ name: 'RdEcnDetail', params: { id } })
}

// ========== 标签映射 ==========

const STATUS_LABELS: Record<EcnStatus, string> = {
  DRAFT: '草稿',
  UNDER_REVIEW: '审批中',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  IMPLEMENTED: '已实施',
  CANCELLED: '已撤回'
}

const STATUS_TAG_TYPES: Record<EcnStatus, ElTagType> = {
  DRAFT: 'info',
  UNDER_REVIEW: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  IMPLEMENTED: 'primary',
  CANCELLED: 'info'
}

const CHANGE_TYPE_LABELS: Record<EcnChangeType, string> = {
  DESIGN: '设计',
  MATERIAL: '物料',
  PROCESS: '工艺',
  DOCUMENT: '文档'
}

const URGENCY_LABELS: Record<EcnUrgency, string> = {
  NORMAL: '普通',
  URGENT: '紧急',
  CRITICAL: '特急'
}

const URGENCY_TAG_TYPES: Record<EcnUrgency, ElTagType> = {
  NORMAL: '',
  URGENT: 'warning',
  CRITICAL: 'danger'
}

function statusLabel(s: EcnStatus) { return STATUS_LABELS[s] ?? s }
function statusTagType(s: EcnStatus) { return STATUS_TAG_TYPES[s] ?? '' }
function changeTypeLabel(t: EcnChangeType) { return CHANGE_TYPE_LABELS[t] ?? t }
function urgencyLabel(u: EcnUrgency) { return URGENCY_LABELS[u] ?? u }
function urgencyTagType(u: EcnUrgency) { return URGENCY_TAG_TYPES[u] ?? '' }

function formatDateTime(s: string) {
  if (!s) return '-'
  const d = new Date(s)
  if (isNaN(d.getTime())) return s
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// ========== 创建对话框 ==========

const createDialogVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive<CreateEcnRequest>({
  title: '',
  changeType: 'DESIGN',
  urgency: 'NORMAL',
  reason: '',
  description: '',
  impactAnalysis: '',
  priority: 0
})

const createRules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  changeType: [{ required: true, message: '请选择变更类型', trigger: 'change' }],
  reason: [{ required: true, message: '请输入变更原因', trigger: 'blur' }],
  description: [{ required: true, message: '请输入变更描述', trigger: 'blur' }]
}

function handleCreate() {
  createDialogVisible.value = true
}

function resetCreateForm() {
  createForm.title = ''
  createForm.changeType = 'DESIGN'
  createForm.urgency = 'NORMAL'
  createForm.reason = ''
  createForm.description = ''
  createForm.impactAnalysis = ''
  createForm.priority = 0
  createForm.targetDate = undefined
  createFormRef.value?.clearValidate()
}

async function handleCreateSubmit() {
  if (!createFormRef.value) return
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  createLoading.value = true
  try {
    const res = await createEcn({
      title: createForm.title,
      changeType: createForm.changeType,
      urgency: createForm.urgency,
      reason: createForm.reason,
      description: createForm.description,
      impactAnalysis: createForm.impactAnalysis || undefined,
      priority: createForm.priority,
      targetDate: createForm.targetDate
    })
    ElMessage.success(`ECN 草稿已创建：${res.data.ecnNumber}`)
    createDialogVisible.value = false
    handleSearch()
  } catch (e) {
    /* ignore */
  } finally {
    createLoading.value = false
  }
}

// ========== 初始化 ==========

onMounted(() => {
  fetchList()
  fetchPending()
})
</script>

<style scoped>
.ecn-container {
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
.pending-banner {
  margin-bottom: 16px;
}
.pending-banner :deep(.el-alert__content) {
  padding: 4px 0;
}
.pending-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
}
.pending-tag {
  cursor: pointer;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
.form-tip {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
