<template>
  <div class="ecn-detail">
    <el-page-header :icon="ArrowLeft" content="返回 ECN 列表" @back="goBack" class="page-header" />

    <!-- 企微通知未接入提示横幅 -->
    <el-alert
      title="企微应用消息通知（任务分配 / 审批结果）当前仅记录日志，未真实推送"
      type="info"
      :closable="false"
      show-icon
      class="notice-banner"
    />

    <el-card v-if="ecn" shadow="never" class="info-card">
      <template #header>
        <div class="card-header">
          <div>
            <h2 class="ecn-title">{{ ecn.title }}</h2>
            <div class="ecn-meta">
              <el-tag :type="statusTagType(ecn.status)" size="default">
                {{ statusLabel(ecn.status) }}
              </el-tag>
              <el-tag :type="urgencyTagType(ecn.urgency)" size="default" effect="plain">
                {{ urgencyLabel(ecn.urgency) }}
              </el-tag>
              <span class="ecn-number">{{ ecn.ecnNumber }}</span>
              <span class="muted">发起人：{{ ecn.requesterName || ecn.requesterUserid }}</span>
            </div>
          </div>
          <div class="card-actions">
            <el-button
              v-if="ecn.status === 'DRAFT' && isOwnOrAdmin"
              type="primary"
              :icon="Promotion"
              :loading="actionLoading.submit"
              @click="handleSubmit"
            >
              提交审批
            </el-button>
            <el-button
              v-if="ecn.status === 'DRAFT' && isOwnOrAdmin"
              type="warning"
              :icon="CircleClose"
              :loading="actionLoading.cancel"
              @click="handleCancel"
            >
              撤回
            </el-button>
            <el-button
              v-if="ecn.status === 'APPROVED' && isOwnOrAdmin"
              type="success"
              :icon="Select"
              :loading="actionLoading.implement"
              @click="handleImplement"
            >
              标记实施完成
            </el-button>
          </div>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="变更类型">{{ changeTypeLabel(ecn.changeType) }}</el-descriptions-item>
        <el-descriptions-item label="目标日期">{{ ecn.targetDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(ecn.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(ecn.updatedAt) }}</el-descriptions-item>
        <el-descriptions-item label="流程实例" :span="2">
          <code v-if="ecn.processInstanceId">{{ ecn.processInstanceId }}</code>
          <span v-else class="muted">-</span>
        </el-descriptions-item>
        <el-descriptions-item label="变更原因" :span="2">
          <pre class="pre">{{ ecn.reason }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="变更描述" :span="2">
          <pre class="pre">{{ ecn.description }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="ecn.impactAnalysis" label="影响分析" :span="2">
          <pre class="pre">{{ ecn.impactAnalysis }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- ========== 我的待办卡片（动态显示） ========== -->
    <el-card v-if="myTask" shadow="never" class="action-card">
      <template #header>
        <span class="card-title">我的待办：{{ myTask.taskDefinitionName || '审批任务' }}</span>
      </template>
      <el-form label-width="80px">
        <el-form-item label="审批意见">
          <el-input
            v-model="approvalComment"
            type="textarea"
            :rows="3"
            placeholder="可填意见，将记录到审批历史"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="success"
            :icon="Check"
            :loading="actionLoading.complete"
            @click="handleComplete(true)"
          >
            同意
          </el-button>
          <el-button
            type="danger"
            :icon="Close"
            :loading="actionLoading.complete"
            @click="handleComplete(false)"
          >
            驳回
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- ========== 审批时间线 ========== -->
    <el-card shadow="never" class="timeline-card">
      <template #header>
        <span class="card-title">审批流转记录</span>
      </template>
      <el-empty v-if="approvals.length === 0" description="尚未提交审批" :image-size="80" />
      <el-timeline v-else>
        <el-timeline-item
          v-for="a in approvals"
          :key="a.id"
          :type="approvalTimelineType(a.status)"
          :timestamp="formatDateTime(a.signedAt || a.createdAt)"
          placement="top"
        >
          <div class="approval-row">
            <el-tag :type="approvalStatusTagType(a.status)" size="small">
              {{ approvalStatusLabel(a.status) }}
            </el-tag>
            <span class="approval-step">第 {{ a.stepOrder }} 步</span>
            <span class="approval-role">{{ a.role || '审批' }}</span>
            <span class="approval-approver">
              {{ a.approverName || a.approverUserid }}
              <span v-if="a.department" class="muted"> · {{ a.department }}</span>
            </span>
          </div>
          <div v-if="a.comment" class="approval-comment">{{ a.comment }}</div>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Promotion, CircleClose, Check, Close, Select } from '@element-plus/icons-vue'
import {
  getEcn,
  listEcnApprovals,
  submitEcn,
  cancelEcn,
  implementEcn,
  listMyPendingEcnTasks,
  claimEcnTask,
  completeEcnTask,
  type EcnChangeVO,
  type EcnApprovalVO,
  type EcnStatus,
  type EcnChangeType,
  type EcnUrgency
} from '@/api/ecn'
import { useUserStore } from '@/stores/user'

type ElTagType = 'primary' | 'success' | 'warning' | 'info' | 'danger' | ''

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const ecnId = computed(() => Number(route.params.id))
const ecn = ref<EcnChangeVO | null>(null)
const approvals = ref<EcnApprovalVO[]>([])
const myTask = ref<{ taskId: string; taskDefinitionName?: string } | null>(null)
const approvalComment = ref('')

const actionLoading = reactive({
  submit: false,
  cancel: false,
  implement: false,
  complete: false
})

const isOwnOrAdmin = computed(() => {
  if (!ecn.value) return false
  return ecn.value.requesterUserid === userStore.userInfo?.userId
    || userStore.isAdmin
})

// ========== 标签映射（跟列表页一致） ==========

const STATUS_LABELS: Record<EcnStatus, string> = {
  DRAFT: '草稿', UNDER_REVIEW: '审批中', APPROVED: '已通过',
  REJECTED: '已驳回', IMPLEMENTED: '已实施', CANCELLED: '已撤回'
}
const STATUS_TAG_TYPES: Record<EcnStatus, ElTagType> = {
  DRAFT: 'info', UNDER_REVIEW: 'warning', APPROVED: 'success',
  REJECTED: 'danger', IMPLEMENTED: 'primary', CANCELLED: 'info'
}
const CHANGE_TYPE_LABELS: Record<EcnChangeType, string> = {
  DESIGN: '设计', MATERIAL: '物料', PROCESS: '工艺', DOCUMENT: '文档'
}
const URGENCY_LABELS: Record<EcnUrgency, string> = {
  NORMAL: '普通', URGENT: '紧急', CRITICAL: '特急'
}
const URGENCY_TAG_TYPES: Record<EcnUrgency, ElTagType> = {
  NORMAL: '', URGENT: 'warning', CRITICAL: 'danger'
}

const APPROVAL_STATUS_LABELS: Record<string, string> = {
  PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回', SKIPPED: '已跳过'
}
const APPROVAL_STATUS_TAG_TYPES: Record<string, ElTagType> = {
  PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', SKIPPED: 'info'
}

function statusLabel(s: EcnStatus) { return STATUS_LABELS[s] ?? s }
function statusTagType(s: EcnStatus) { return STATUS_TAG_TYPES[s] ?? '' }
function changeTypeLabel(t: EcnChangeType) { return CHANGE_TYPE_LABELS[t] ?? t }
function urgencyLabel(u: EcnUrgency) { return URGENCY_LABELS[u] ?? u }
function urgencyTagType(u: EcnUrgency) { return URGENCY_TAG_TYPES[u] ?? '' }
function approvalStatusLabel(s: string) { return APPROVAL_STATUS_LABELS[s] ?? s }
function approvalStatusTagType(s: string) { return APPROVAL_STATUS_TAG_TYPES[s] ?? '' }
function approvalTimelineType(s: string): ElTagType {
  return approvalStatusTagType(s) as ElTagType
}

function formatDateTime(s: string) {
  if (!s) return '-'
  const d = new Date(s)
  if (isNaN(d.getTime())) return s
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// ========== 数据加载 ==========

async function fetchAll() {
  try {
    const res = await getEcn(ecnId.value)
    ecn.value = res.data
  } catch (e) {
    ElMessage.error('ECN 不存在或无权查看')
    router.push({ name: 'RdEcn' })
    return
  }
  try {
    const res = await listEcnApprovals(ecnId.value)
    approvals.value = res.data
  } catch (e) {
    approvals.value = []
  }
  await findMyTask()
}

/**
 * 当前用户在此 ECN 的待办（如果是 assignee）
 */
async function findMyTask() {
  if (!ecn.value || ecn.value.status !== 'UNDER_REVIEW') {
    myTask.value = null
    return
  }
  // 后端 /my-pending 拿当前用户所有 ECN 待办
  // 检查其中是否包含当前 ECN
  try {
    const pending = (await listMyPendingEcnTasks()).data
    const found = pending.find(t => t.id === ecnId.value)
    if (found) {
      // 后端 listMyPendingEcnTasks 返回 EcnChangeVO[] 不含 taskId
      // taskId 需要另存: 从 approvals 表查 PENDING 且 assignee=我
      const myApproval = approvals.value.find(
        a => a.status === 'PENDING' && a.approverUserid === userStore.userInfo?.userId
      )
      if (myApproval?.taskId) {
        myTask.value = { taskId: myApproval.taskId, taskDefinitionName: myApproval.role }
        return
      }
    }
  } catch (e) {
    // ignore
  }
  myTask.value = null
}

// ========== 操作 ==========

async function handleSubmit() {
  try {
    await ElMessageBox.confirm(
      `确认提交 ECN「${ecn.value?.title}」进入审批流程？`,
      '提交审批',
      { type: 'info', confirmButtonText: '提交', cancelButtonText: '取消' }
    )
  } catch { return }
  actionLoading.submit = true
  try {
    const res = await submitEcn(ecnId.value)
    ElMessage.success('已提交审批')
    ecn.value = res.data
    await fetchAll()
  } catch (e) {
    /* ignore */
  } finally {
    actionLoading.submit = false
  }
}

async function handleCancel() {
  try {
    await ElMessageBox.confirm(
      '撤回后此 ECN 将变更为"已撤回"状态，确定继续？',
      '撤回 ECN',
      { type: 'warning', confirmButtonText: '撤回', cancelButtonText: '取消' }
    )
  } catch { return }
  actionLoading.cancel = true
  try {
    const res = await cancelEcn(ecnId.value)
    ElMessage.success('已撤回')
    ecn.value = res.data
  } catch (e) {
    /* ignore */
  } finally {
    actionLoading.cancel = false
  }
}

async function handleImplement() {
  try {
    await ElMessageBox.confirm(
      '确认此 ECN 已实施完成？此操作不可撤销。',
      '标记实施完成',
      { type: 'success', confirmButtonText: '确认', cancelButtonText: '取消' }
    )
  } catch { return }
  actionLoading.implement = true
  try {
    const res = await implementEcn(ecnId.value)
    ElMessage.success('已标记实施完成')
    ecn.value = res.data
  } catch (e) {
    /* ignore */
  } finally {
    actionLoading.implement = false
  }
}

async function handleComplete(approved: boolean) {
  if (!myTask.value) {
    ElMessage.error('未找到待办任务')
    return
  }
  const action = approved ? '同意' : '驳回'
  try {
    await ElMessageBox.confirm(
      `确认${action}此审批？${!approved ? '驳回将终止整个审批流程。' : ''}`,
      `${action}审批`,
      {
        type: approved ? 'success' : 'warning',
        confirmButtonText: action,
        cancelButtonText: '取消'
      }
    )
  } catch { return }
  actionLoading.complete = true
  try {
    await completeEcnTask(
      myTask.value.taskId,
      approved,
      approvalComment.value.trim() || undefined
    )
    ElMessage.success(`已${action}`)
    approvalComment.value = ''
    await fetchAll()
  } catch (e) {
    /* ignore */
  } finally {
    actionLoading.complete = false
  }
}

function goBack() {
  router.push({ name: 'RdEcn' })
}

onMounted(() => {
  fetchAll()
})
</script>

<style scoped>
.ecn-detail {
  padding: 0;
}
.page-header {
  margin-bottom: 16px;
}
.notice-banner {
  margin-bottom: 16px;
}
.info-card,
.action-card,
.timeline-card {
  border-radius: 6px;
  margin-bottom: 16px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}
.ecn-title {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
.ecn-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  font-size: 13px;
}
.ecn-number {
  color: #909399;
  font-family: 'SFMono-Regular', Consolas, monospace;
}
.muted {
  color: #909399;
}
.card-title {
  font-weight: 600;
  color: #303133;
}
.card-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
.pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  color: #606266;
  line-height: 1.6;
}
.approval-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.approval-step {
  color: #909399;
  font-size: 13px;
}
.approval-role {
  color: #606266;
  font-size: 13px;
}
.approval-approver {
  color: #303133;
  font-size: 14px;
}
.approval-comment {
  margin-top: 4px;
  color: #606266;
  font-size: 13px;
  background: #f5f7fa;
  padding: 6px 10px;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
