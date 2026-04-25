<template>
  <PermissionGate :roles="[1]">
    <section class="coupon-page">
      <div class="surface-card coupon-toolbar">
        <div>
          <p class="eyebrow">Coupon Push Task</p>
          <h3>推送任务管理</h3>
          <p class="muted">面向商家店铺的批量发券任务，按当前店铺编号隔离任务、模板和发放记录。</p>
        </div>
        <div class="toolbar-actions">
          <el-tag type="success">{{ authStore.shopNumber || '当前店铺' }}</el-tag>
          <el-button type="primary" :icon="Plus" @click="openCreateDrawer">新建推送任务</el-button>
        </div>
      </div>

      <div class="role-policy-grid">
        <article class="surface-card policy-card">
          <ShieldCheck :size="20" />
          <div>
            <strong>商家任务边界</strong>
            <p>仅允许商家创建和查看自己店铺的推送任务，任务必须绑定当前店铺可用的优惠券模板。</p>
          </div>
        </article>
        <article class="surface-card policy-card">
          <FileSpreadsheet :size="20" />
          <div>
            <strong>名单文件规则</strong>
            <p>后端当前接收 Excel 文件路径并读取发券名单，前端负责提交路径、通知方式和发送时间。</p>
          </div>
        </article>
      </div>

      <div class="surface-card filter-panel">
        <el-form :model="filters" label-position="top">
          <div class="filter-grid">
            <el-form-item label="批次 ID">
              <el-input v-model.trim="filters.batchId" placeholder="按批次 ID 查询" clearable />
            </el-form-item>
            <el-form-item label="任务名称">
              <el-input v-model.trim="filters.taskName" placeholder="按任务名称查询" clearable />
            </el-form-item>
            <el-form-item label="优惠券模板 ID">
              <el-input v-model.trim="filters.couponTemplateId" placeholder="按模板 ID 查询" clearable />
            </el-form-item>
            <el-form-item label="执行状态">
              <el-select v-model="filters.status" placeholder="全部状态" clearable>
                <el-option v-for="item in taskStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </div>
          <div class="filter-actions">
            <el-button :icon="RefreshCw" @click="resetFilters">重置</el-button>
            <el-button type="primary" :icon="Search" @click="loadTasks">查询</el-button>
          </div>
        </el-form>
      </div>

      <div class="surface-card table-card">
        <el-table v-loading="loading" :data="tasks" row-key="id" stripe>
          <el-table-column prop="batchId" label="批次 ID" min-width="180" show-overflow-tooltip />
          <el-table-column prop="taskName" label="任务名称" min-width="180" show-overflow-tooltip />
          <el-table-column label="优惠券模板" min-width="210" show-overflow-tooltip>
            <template #default="{ row }">
              <div class="task-template-cell">
                <strong>{{ getTemplateName(row.couponTemplateId) }}</strong>
                <span>{{ row.couponTemplateId }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="发送数量" width="110">
            <template #default="{ row }">{{ row.sendNum ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="通知方式" min-width="150">
            <template #default="{ row }">{{ formatCouponTaskNotifyTypes(row.notifyType) }}</template>
          </el-table-column>
          <el-table-column label="发送方式" width="110">
            <template #default="{ row }">{{ getCouponTaskSendTypeText(row.sendType) }}</template>
          </el-table-column>
          <el-table-column prop="sendTime" label="发送时间" min-width="170" show-overflow-tooltip />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="couponTaskStatusTagType(row.status)">{{ getCouponTaskStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="completionTime" label="完成时间" min-width="170" show-overflow-tooltip />
          <el-table-column prop="operatorId" label="操作人" width="120" show-overflow-tooltip />
          <el-table-column label="操作" fixed="right" width="110">
            <template #default="{ row }">
              <el-button link type="primary" :icon="Eye" @click="openDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-footer">
          <el-pagination
            v-model:current-page="pagination.current"
            v-model:page-size="pagination.size"
            :total="pagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadTasks"
            @current-change="loadTasks"
          />
        </div>
      </div>
    </section>

    <el-drawer v-model="formDrawerVisible" title="新建推送任务" size="620px" destroy-on-close>
      <el-form ref="taskFormRef" :model="taskForm" :rules="taskRules" label-position="top">
        <el-alert
          title="提交前请确认名单文件可被后端服务读取"
          description="当前接口接收 Excel 文件路径，例如项目 tmp 目录下的发券名单文件；浏览器不会直接上传文件内容。"
          type="info"
          show-icon
          :closable="false"
          class="form-alert"
        />

        <div class="form-section-title">基础信息</div>
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model.trim="taskForm.taskName" placeholder="例如：重邮校园满减券周末推送" clearable />
        </el-form-item>
        <el-form-item label="优惠券模板" prop="couponTemplateId">
          <el-select
            v-model="taskForm.couponTemplateId"
            :loading="templateLoading"
            placeholder="选择当前店铺可用模板"
            filterable
            clearable
          >
            <el-option
              v-for="template in activeTemplates"
              :key="template.id"
              :label="`${template.name}（${formatCouponBenefit(template.type, template.consumeRule)}）`"
              :value="String(template.id)"
            >
              <div class="task-template-option">
                <span>{{ template.name }}</span>
                <small>{{ template.id }} · {{ formatCouponBenefit(template.type, template.consumeRule) }}</small>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="名单文件路径" prop="fileAddress">
          <el-input
            v-model.trim="taskForm.fileAddress"
            placeholder="E:\IdeaProjects\mall-cqupt-lqy11\tmp\oneCoupon任务推送Excel.xlsx"
            clearable
          />
        </el-form-item>

        <div class="form-section-title">发送配置</div>
        <el-form-item label="通知方式" prop="notifyTypes">
          <el-checkbox-group v-model="taskForm.notifyTypes">
            <el-checkbox-button v-for="item in notifyTypeOptions" :key="item.value" :label="item.value">
              {{ item.label }}
            </el-checkbox-button>
          </el-checkbox-group>
        </el-form-item>
        <div class="two-column-form">
          <el-form-item label="发送方式" prop="sendType">
            <el-radio-group v-model="taskForm.sendType">
              <el-radio-button :label="0">立即发送</el-radio-button>
              <el-radio-button :label="1">定时发送</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="taskForm.sendType === 1" label="定时发送时间" prop="sendTime">
            <el-date-picker
              v-model="taskForm.sendTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="选择发送时间"
              style="width: 100%"
            />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="formDrawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingTask" :icon="Send" @click="submitTask">提交任务</el-button>
      </template>
    </el-drawer>

    <el-drawer v-model="detailDrawerVisible" title="推送任务详情" size="520px">
      <el-skeleton v-if="detailLoading" :rows="7" animated />
      <div v-else-if="selectedTask" class="detail-panel">
        <div class="task-summary-card">
          <span>{{ getCouponTaskSendTypeText(selectedTask.sendType) }} · {{ getCouponTaskStatusText(selectedTask.status) }}</span>
          <strong>{{ selectedTask.taskName }}</strong>
          <p>{{ formatCouponTaskNotifyTypes(selectedTask.notifyType) }}</p>
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="任务 ID">{{ selectedTask.id || '-' }}</el-descriptions-item>
          <el-descriptions-item label="批次 ID">{{ selectedTask.batchId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="任务名称">{{ selectedTask.taskName }}</el-descriptions-item>
          <el-descriptions-item label="优惠券模板">
            {{ getTemplateName(selectedTask.couponTemplateId) }}（{{ selectedTask.couponTemplateId }}）
          </el-descriptions-item>
          <el-descriptions-item label="名单文件">{{ selectedTask.fileAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="失败文件">{{ selectedTask.failFileAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发送数量">{{ selectedTask.sendNum ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="通知方式">{{ formatCouponTaskNotifyTypes(selectedTask.notifyType) }}</el-descriptions-item>
          <el-descriptions-item label="发送方式">{{ getCouponTaskSendTypeText(selectedTask.sendType) }}</el-descriptions-item>
          <el-descriptions-item label="发送时间">{{ selectedTask.sendTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="任务状态">{{ getCouponTaskStatusText(selectedTask.status) }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ selectedTask.completionTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="操作人">{{ selectedTask.operatorId || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <el-empty v-else description="未查询到推送任务" />
    </el-drawer>
  </PermissionGate>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Eye, FileSpreadsheet, Plus, RefreshCw, Search, Send, ShieldCheck } from 'lucide-vue-next'
import { couponTaskApi, couponTemplateApi } from '@/api/coupon'
import PermissionGate from '@/components/auth/PermissionGate.vue'
import { useAuthStore } from '@/stores/auth'
import type {
  CouponTask,
  CouponTaskCreatePayload,
  CouponTaskNotifyType,
  CouponTaskSendType,
  CouponTaskStatus,
  CouponTemplate
} from '@/types/coupon'
import {
  couponTaskStatusTagType,
  formatCouponBenefit,
  formatCouponTaskNotifyTypes,
  getCouponTaskSendTypeText,
  getCouponTaskStatusText
} from '@/utils/coupon'

interface TaskForm {
  taskName: string
  couponTemplateId: string
  fileAddress: string
  notifyTypes: CouponTaskNotifyType[]
  sendType: CouponTaskSendType
  sendTime: string
}

const authStore = useAuthStore()

const taskStatusOptions: Array<{ label: string; value: CouponTaskStatus }> = [
  { label: '待执行', value: 0 },
  { label: '执行中', value: 1 },
  { label: '执行失败', value: 2 },
  { label: '执行成功', value: 3 },
  { label: '已取消', value: 4 }
]

const notifyTypeOptions: Array<{ label: string; value: CouponTaskNotifyType }> = [
  { label: '站内信', value: '0' },
  { label: '弹框推送', value: '1' },
  { label: '邮箱', value: '2' },
  { label: '短信', value: '3' }
]

const loading = ref(false)
const templateLoading = ref(false)
const tasks = ref<CouponTask[]>([])
const templates = ref<CouponTemplate[]>([])
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const filters = reactive<{
  batchId: string
  taskName: string
  couponTemplateId: string
  status?: CouponTaskStatus
}>({
  batchId: '',
  taskName: '',
  couponTemplateId: '',
  status: undefined
})

const taskFormRef = ref<FormInstance>()
const formDrawerVisible = ref(false)
const savingTask = ref(false)
const taskForm = reactive<TaskForm>(createDefaultTaskForm())

const detailDrawerVisible = ref(false)
const detailLoading = ref(false)
const selectedTask = ref<CouponTask | null>(null)

const activeTemplates = computed(() => templates.value.filter((template) => template.id && template.status !== 1))

const taskRules: FormRules<TaskForm> = {
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  couponTemplateId: [{ required: true, message: '请选择优惠券模板', trigger: 'change' }],
  fileAddress: [{ required: true, message: '请输入名单文件路径', trigger: 'blur' }],
  notifyTypes: [
    {
      type: 'array',
      required: true,
      min: 1,
      message: '请至少选择一种通知方式',
      trigger: 'change'
    }
  ],
  sendType: [{ required: true, message: '请选择发送方式', trigger: 'change' }],
  sendTime: [
    {
      validator: (_rule, value, callback) => {
        if (taskForm.sendType === 1 && !value) {
          callback(new Error('请选择定时发送时间'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ]
}

watch(
  () => taskForm.sendType,
  (sendType) => {
    if (sendType === 0) {
      taskForm.sendTime = ''
    }
  }
)

function createDefaultTaskForm(): TaskForm {
  return {
    taskName: '',
    couponTemplateId: '',
    fileAddress: '',
    notifyTypes: ['0', '3'],
    sendType: 0,
    sendTime: ''
  }
}

function resetTaskForm() {
  Object.assign(taskForm, createDefaultTaskForm())
}

function openCreateDrawer() {
  resetTaskForm()
  formDrawerVisible.value = true
  if (!templates.value.length) {
    void loadTemplates()
  }
}

async function loadTasks() {
  loading.value = true
  try {
    const page = await couponTaskApi.page({
      current: pagination.current,
      size: pagination.size,
      batchId: filters.batchId || undefined,
      taskName: filters.taskName || undefined,
      couponTemplateId: filters.couponTemplateId || undefined,
      status: typeof filters.status === 'number' ? filters.status : undefined
    })
    tasks.value = page.records || []
    pagination.total = Number(page.total || 0)
    pagination.current = Number(page.current || pagination.current)
    pagination.size = Number(page.size || pagination.size)
  } finally {
    loading.value = false
  }
}

async function loadTemplates() {
  templateLoading.value = true
  try {
    const page = await couponTemplateApi.page({
      current: 1,
      size: 50,
      source: 0
    })
    templates.value = page.records || []
  } finally {
    templateLoading.value = false
  }
}

function resetFilters() {
  filters.batchId = ''
  filters.taskName = ''
  filters.couponTemplateId = ''
  filters.status = undefined
  pagination.current = 1
  void loadTasks()
}

function buildTaskPayload(): CouponTaskCreatePayload {
  return {
    taskName: taskForm.taskName,
    fileAddress: taskForm.fileAddress,
    notifyType: taskForm.notifyTypes.join(','),
    couponTemplateId: taskForm.couponTemplateId,
    sendType: taskForm.sendType,
    sendTime: taskForm.sendType === 1 ? taskForm.sendTime : undefined
  }
}

async function submitTask() {
  await taskFormRef.value?.validate()
  savingTask.value = true
  try {
    await couponTaskApi.create(buildTaskPayload())
    ElMessage.success('推送任务已创建')
    pagination.current = 1
    formDrawerVisible.value = false
    await loadTasks()
  } finally {
    savingTask.value = false
  }
}

async function openDetail(row: CouponTask) {
  selectedTask.value = row
  detailDrawerVisible.value = true
  if (!row.id) {
    ElMessage.warning('当前任务缺少 ID，仅展示列表信息')
    return
  }
  detailLoading.value = true
  try {
    selectedTask.value = await couponTaskApi.detail(String(row.id))
  } finally {
    detailLoading.value = false
  }
}

function getTemplateName(couponTemplateId?: string) {
  if (!couponTemplateId) return '-'
  const template = templates.value.find((item) => String(item.id) === String(couponTemplateId))
  return template?.name || '模板未缓存'
}

onMounted(() => {
  void loadTasks()
  void loadTemplates()
})
</script>

<style scoped>
.task-template-cell,
.task-template-option {
  display: grid;
  gap: 3px;
}

.task-template-cell span,
.task-template-option small {
  color: var(--color-muted);
  font-size: 12px;
}

.task-summary-card {
  padding: 22px;
  border-radius: 8px;
  color: #fff;
  background:
    radial-gradient(circle at 18% 18%, rgba(0, 212, 255, 0.3), transparent 26%),
    var(--gradient-brand-deep);
  box-shadow: var(--shadow-brand);
}

.task-summary-card span {
  font-size: 13px;
  opacity: 0.86;
}

.task-summary-card strong {
  display: block;
  margin: 8px 0;
  font-size: 24px;
  line-height: 1.35;
}

.task-summary-card p {
  margin: 0;
  opacity: 0.9;
}
</style>
