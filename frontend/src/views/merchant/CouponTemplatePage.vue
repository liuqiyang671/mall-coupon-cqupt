<template>
  <PermissionGate :roles="[0, 1]">
    <section class="coupon-page">
      <div class="surface-card coupon-toolbar">
        <div>
          <p class="eyebrow">Coupon Template</p>
          <h3>{{ pageTitle }}</h3>
          <p class="muted">{{ pageDescription }}</p>
        </div>
        <div class="toolbar-actions">
          <el-tag type="info">{{ authStore.roleLabel }}</el-tag>
          <el-button type="primary" :icon="Plus" @click="openCreateDrawer">创建{{ currentSourceLabel }}</el-button>
        </div>
      </div>

      <div class="role-policy-grid">
        <article class="surface-card policy-card">
          <ShieldCheck :size="20" />
          <div>
            <strong>{{ authStore.isPlatform ? '平台级控制能力' : '商家券边界' }}</strong>
            <p v-if="authStore.isPlatform">平台人员只能管理平台券模板，可配置适用商家和发放方式，不直接修改商家券。</p>
            <p v-else>商家只能管理自己店铺的商家券，可设置商品范围、门槛和发行量，不能创建平台券。</p>
          </div>
        </article>
        <article class="surface-card policy-card">
          <CircleDollarSign :size="20" />
          <div>
            <strong>优惠规则</strong>
            <p>支持满减券、折扣券、立减券，并统一维护使用门槛、优惠上限、有效期和库存。</p>
          </div>
        </article>
      </div>

      <div class="surface-card filter-panel">
        <el-form :model="filters" label-position="top">
          <div class="filter-grid">
            <el-form-item label="券归属">
              <el-input :model-value="currentSourceLabel" disabled />
            </el-form-item>
            <el-form-item label="券名称">
              <el-input v-model.trim="filters.name" placeholder="按名称搜索" clearable />
            </el-form-item>
            <el-form-item label="优惠类型">
              <el-select v-model="filters.type" placeholder="全部类型" clearable>
                <el-option v-for="item in couponTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="优惠对象">
              <el-select v-model="filters.target" placeholder="全部对象" clearable>
                <el-option v-for="item in couponTargetOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="商品编码">
              <el-input v-model.trim="filters.goods" placeholder="商品专属券可筛选" clearable />
            </el-form-item>
          </div>
          <div class="filter-actions">
            <el-button :icon="RefreshCw" @click="resetFilters">重置</el-button>
            <el-button type="primary" :icon="Search" @click="loadTemplates">查询</el-button>
          </div>
        </el-form>
      </div>

      <div class="surface-card table-card">
        <el-table v-loading="loading" :data="templates" row-key="id" stripe>
          <el-table-column prop="name" label="优惠券名称" min-width="180" show-overflow-tooltip />
          <el-table-column label="归属" width="100">
            <template #default="{ row }">
              <el-tag :type="row.source === 1 ? 'warning' : 'success'">{{ getCouponSourceText(row.source) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="110">
            <template #default="{ row }">
              <el-tag>{{ getCouponTypeText(row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="优惠" min-width="130">
            <template #default="{ row }">{{ formatCouponBenefit(row.type, row.consumeRule) }}</template>
          </el-table-column>
          <el-table-column label="适用范围" min-width="170" show-overflow-tooltip>
            <template #default="{ row }">
              {{ getCouponTargetText(row.target) }}<span v-if="row.goods">：{{ row.goods }}</span>
            </template>
          </el-table-column>
          <el-table-column v-if="authStore.isPlatform" label="适用商家" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">{{ formatApplicableMerchant(row.receiveRule) }}</template>
          </el-table-column>
          <el-table-column v-if="authStore.isPlatform" label="发放方式" width="110">
            <template #default="{ row }">{{ formatDistributionMode(row.receiveRule) }}</template>
          </el-table-column>
          <el-table-column prop="stock" label="库存" width="100" />
          <el-table-column label="有效期" min-width="260">
            <template #default="{ row }">
              <span>{{ row.validStartTime || '-' }}</span>
              <span class="date-split">至</span>
              <span>{{ row.validEndTime || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="couponStatusTagType(row.status)">{{ getCouponStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" width="260">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
              <el-button link type="primary" :disabled="row.status !== 0" @click="openEdit(row)">编辑</el-button>
              <el-button link type="primary" :disabled="row.status !== 0" @click="openIncrease(row)">增发</el-button>
              <el-button link type="danger" :disabled="row.status !== 0" @click="terminateTemplate(row)">下架</el-button>
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
            @size-change="loadTemplates"
            @current-change="loadTemplates"
          />
        </div>
      </div>
    </section>

    <el-drawer v-model="formDrawerVisible" :title="formDrawerTitle" size="600px" destroy-on-close>
      <el-form ref="templateFormRef" :model="templateForm" :rules="templateRules" label-position="top">
        <el-alert :title="formPolicyTitle" :description="formPolicyDescription" type="info" show-icon :closable="false" class="form-alert" />

        <div class="form-section-title">基础信息</div>
        <el-form-item label="券归属">
          <el-input :model-value="currentSourceLabel" disabled />
        </el-form-item>
        <el-form-item label="优惠券名称" prop="name">
          <el-input v-model.trim="templateForm.name" placeholder="例如：重邮校园满减券" clearable />
        </el-form-item>
        <div class="two-column-form">
          <el-form-item label="优惠类型" prop="type">
            <el-segmented v-model="templateForm.type" :options="couponTypeOptions" />
          </el-form-item>
          <el-form-item label="发行库存" prop="stock">
            <el-input-number v-model="templateForm.stock" :min="1" :step="1" step-strictly controls-position="right" />
          </el-form-item>
        </div>

        <div class="form-section-title">{{ authStore.isPlatform ? '平台适用范围' : '店铺与商品范围' }}</div>
        <template v-if="authStore.isPlatform">
          <el-form-item label="适用商家" prop="receiveRule.applicableMerchantScope">
            <el-radio-group v-model="templateForm.receiveRule.applicableMerchantScope">
              <el-radio-button label="ALL">全部商家</el-radio-button>
              <el-radio-button label="SPECIFIED">指定商家</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="templateForm.receiveRule.applicableMerchantScope === 'SPECIFIED'" label="指定商家编号" prop="receiveRule.applicableShopNumbers">
            <el-input
              v-model.trim="templateForm.receiveRule.applicableShopNumbers"
              type="textarea"
              :rows="2"
              placeholder="多个店铺编号用英文逗号分隔"
            />
          </el-form-item>
          <el-form-item label="发放方式" prop="receiveRule.distributionMode">
            <el-radio-group v-model="templateForm.receiveRule.distributionMode">
              <el-radio-button label="RECEIVE">领取</el-radio-button>
              <el-radio-button label="AUTO">自动发放</el-radio-button>
              <el-radio-button label="ACTIVITY">活动发放</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item label="所属店铺">
            <el-input :model-value="authStore.shopDisplayName" disabled />
          </el-form-item>
        </template>

        <el-form-item label="优惠对象" prop="target">
          <el-radio-group v-model="templateForm.target">
            <el-radio-button :label="1">全店通用</el-radio-button>
            <el-radio-button :label="0">商品专属</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="templateForm.target === 0" label="商品编码" prop="goods">
          <el-input v-model.trim="templateForm.goods" placeholder="多个商品可用英文逗号分隔" clearable />
        </el-form-item>

        <div class="form-section-title">优惠规则</div>
        <div class="two-column-form">
          <el-form-item v-if="templateForm.type !== 0" label="使用门槛" prop="consumeRule.termsOfUse">
            <el-input-number v-model="templateForm.consumeRule.termsOfUse" :min="0" :precision="2" controls-position="right" />
          </el-form-item>
          <el-form-item label="优惠金额 / 上限" prop="consumeRule.maximumDiscountAmount">
            <el-input-number v-model="templateForm.consumeRule.maximumDiscountAmount" :min="0.01" :precision="2" controls-position="right" />
          </el-form-item>
          <el-form-item v-if="templateForm.type === 2" label="折扣" prop="consumeRule.discountRate">
            <el-input-number v-model="templateForm.consumeRule.discountRate" :min="0.1" :max="9.9" :precision="1" controls-position="right" />
          </el-form-item>
        </div>
        <el-form-item label="不可用说明" prop="consumeRule.explanationOfUnmetConditions">
          <el-input v-model.trim="templateForm.consumeRule.explanationOfUnmetConditions" placeholder="例如：未达到优惠券使用门槛" clearable />
        </el-form-item>

        <div class="form-section-title">领取与有效期</div>
        <el-form-item label="每人限领" prop="receiveRule.limitPerPerson">
          <el-input-number v-model="templateForm.receiveRule.limitPerPerson" :min="1" :step="1" step-strictly controls-position="right" />
        </el-form-item>
        <el-form-item label="使用说明" prop="receiveRule.usageInstructions">
          <el-input v-model.trim="templateForm.receiveRule.usageInstructions" placeholder="例如：仅限邮惠券商城活动商品使用" clearable />
        </el-form-item>
        <el-form-item label="有效期" prop="validRange">
          <el-date-picker
            v-model="templateForm.validRange"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            range-separator="至"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDrawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingTemplate" @click="submitTemplate">{{ submitButtonText }}</el-button>
      </template>
    </el-drawer>

    <el-drawer v-model="detailDrawerVisible" title="优惠券模板详情" size="480px">
      <el-skeleton v-if="detailLoading" :rows="6" animated />
      <div v-else-if="selectedTemplate" class="detail-panel">
        <div class="coupon-preview-card">
          <span>{{ getCouponSourceText(selectedTemplate.source) }} · {{ getCouponTypeText(selectedTemplate.type) }}</span>
          <strong>{{ formatCouponBenefit(selectedTemplate.type, selectedTemplate.consumeRule) }}</strong>
          <p>{{ selectedTemplate.name }}</p>
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="模板 ID">{{ selectedTemplate.id || '-' }}</el-descriptions-item>
          <el-descriptions-item label="来源">{{ getCouponSourceText(selectedTemplate.source) }}</el-descriptions-item>
          <el-descriptions-item label="适用范围">{{ getCouponTargetText(selectedTemplate.target) }} {{ selectedTemplate.goods || '' }}</el-descriptions-item>
          <el-descriptions-item v-if="selectedTemplate.source === 1" label="适用商家">{{ formatApplicableMerchant(selectedTemplate.receiveRule) }}</el-descriptions-item>
          <el-descriptions-item v-if="selectedTemplate.source === 1" label="发放方式">{{ formatDistributionMode(selectedTemplate.receiveRule) }}</el-descriptions-item>
          <el-descriptions-item label="库存">{{ selectedTemplate.stock }}</el-descriptions-item>
          <el-descriptions-item label="领取规则">{{ formatReceiveLimit(selectedTemplate.receiveRule) }}</el-descriptions-item>
          <el-descriptions-item label="有效期">{{ selectedTemplate.validStartTime }} 至 {{ selectedTemplate.validEndTime }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ getCouponStatusText(selectedTemplate.status) }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <el-empty v-else description="未查询到优惠券模板" />
    </el-drawer>

    <el-dialog v-model="increaseDialogVisible" title="追加发行量" width="420px">
      <el-form label-position="top">
        <el-form-item label="优惠券模板">
          <el-input :model-value="increaseTarget?.name || '-'" disabled />
        </el-form-item>
        <el-form-item label="追加数量">
          <el-input-number v-model="increaseNumber" :min="1" :step="1" step-strictly controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="increaseDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="increasing" @click="submitIncrease">确认追加</el-button>
      </template>
    </el-dialog>
  </PermissionGate>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { CircleDollarSign, Plus, RefreshCw, Search, ShieldCheck } from 'lucide-vue-next'
import { couponTemplateApi } from '@/api/coupon'
import PermissionGate from '@/components/auth/PermissionGate.vue'
import { useAuthStore } from '@/stores/auth'
import type {
  ApplicableMerchantScope,
  ConsumeRuleDraft,
  CouponSource,
  CouponTarget,
  CouponTemplate,
  CouponTemplateSavePayload,
  CouponType,
  DistributionMode,
  ReceiveRuleDraft
} from '@/types/coupon'
import {
  buildConsumeRule,
  buildReceiveRule,
  couponStatusTagType,
  formatApplicableMerchant,
  formatCouponBenefit,
  formatDistributionMode,
  formatReceiveLimit,
  getCouponSourceText,
  getCouponStatusText,
  getCouponTargetText,
  getCouponTypeText,
  parseJsonObject
} from '@/utils/coupon'

interface TemplateForm {
  name: string
  source: CouponSource
  target: CouponTarget
  goods: string
  type: CouponType
  validRange: string[]
  stock: number
  receiveRule: ReceiveRuleDraft
  consumeRule: ConsumeRuleDraft
}

const authStore = useAuthStore()

const currentSource = computed<CouponSource>(() => (authStore.isPlatform ? 1 : 0))
const currentSourceLabel = computed(() => getCouponSourceText(currentSource.value))
const pageTitle = computed(() => (authStore.isPlatform ? '平台券模板管理' : '商家券模板管理'))
const pageDescription = computed(() =>
  authStore.isPlatform
    ? '平台人员可创建、编辑、下架平台券，并配置适用商家和发放方式。'
    : '商家可创建、编辑、下架自己店铺的商家券，并维护商品范围、使用门槛和发行量。'
)
const formPolicyTitle = computed(() => (authStore.isPlatform ? '平台券不能直接修改商家券' : '商家券仅限当前店铺'))
const formPolicyDescription = computed(() =>
  authStore.isPlatform
    ? '当前表单固定创建平台券；适用商家可选全部或指定店铺，发放方式支持领取、自动发放、活动发放。'
    : '当前表单固定创建店铺券；商家不能创建平台券，也不能使用或修改其他商家的优惠券。'
)

const couponTypeOptions = [
  { label: '立减券', value: 0 },
  { label: '满减券', value: 1 },
  { label: '折扣券', value: 2 }
] as const

const couponTargetOptions = [
  { label: '商品专属', value: 0 },
  { label: '全店通用', value: 1 }
] as const

const loading = ref(false)
const templates = ref<CouponTemplate[]>([])
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const filters = reactive<{
  name: string
  type?: CouponType
  target?: CouponTarget
  goods: string
}>({
  name: '',
  type: undefined,
  target: undefined,
  goods: ''
})

const templateFormRef = ref<FormInstance>()
const formDrawerVisible = ref(false)
const savingTemplate = ref(false)
const editingTemplateId = ref('')
const templateForm = reactive<TemplateForm>(createDefaultForm())

const detailDrawerVisible = ref(false)
const detailLoading = ref(false)
const selectedTemplate = ref<CouponTemplate | null>(null)

const increaseDialogVisible = ref(false)
const increasing = ref(false)
const increaseNumber = ref(100)
const increaseTarget = ref<CouponTemplate | null>(null)

const formDrawerTitle = computed(() => `${editingTemplateId.value ? '编辑' : '创建'}${currentSourceLabel.value}模板`)
const submitButtonText = computed(() => (editingTemplateId.value ? '保存修改' : '提交创建'))

const templateRules: FormRules<TemplateForm> = {
  name: [{ required: true, message: '请输入优惠券名称', trigger: 'blur' }],
  source: [{ required: true, message: '请选择券来源', trigger: 'change' }],
  target: [{ required: true, message: '请选择优惠对象', trigger: 'change' }],
  goods: [
    {
      validator: (_rule, value, callback) => {
        templateForm.target === 0 && !value ? callback(new Error('请输入商品编码')) : callback()
      },
      trigger: 'blur'
    }
  ],
  type: [{ required: true, message: '请选择优惠类型', trigger: 'change' }],
  stock: [{ required: true, message: '请输入发行库存', trigger: 'blur' }],
  'receiveRule.applicableShopNumbers': [
    {
      validator: (_rule, value, callback) => {
        if (authStore.isPlatform && templateForm.receiveRule.applicableMerchantScope === 'SPECIFIED' && !value) {
          callback(new Error('请输入指定商家编号'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  validRange: [
    {
      validator: (_rule, value, callback) => {
        if (!value || value.length !== 2) {
          callback(new Error('请选择有效期'))
          return
        }
        if (value[0] >= value[1]) {
          callback(new Error('结束时间必须晚于开始时间'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ]
}

watch(
  () => templateForm.type,
  (type) => {
    if (type === 0) {
      templateForm.consumeRule.termsOfUse = 0
    }
    if (type !== 2) {
      templateForm.consumeRule.discountRate = undefined
    }
  }
)

watch(
  () => templateForm.target,
  (target) => {
    if (target === 1) {
      templateForm.goods = ''
    }
  }
)

function createDefaultForm(): TemplateForm {
  const platform = authStore.isPlatform
  return {
    name: '',
    source: currentSource.value,
    target: 1,
    goods: '',
    type: 1,
    validRange: [],
    stock: 100,
    receiveRule: {
      limitPerPerson: 1,
      usageInstructions: platform ? '平台活动券按页面展示规则使用' : '仅限当前店铺活动商品使用',
      distributionMode: (platform ? 'RECEIVE' : undefined) as DistributionMode | undefined,
      applicableMerchantScope: (platform ? 'ALL' : 'SPECIFIED') as ApplicableMerchantScope,
      applicableShopNumbers: platform ? '' : authStore.shopNumber || ''
    },
    consumeRule: {
      termsOfUse: 100,
      maximumDiscountAmount: 10,
      explanationOfUnmetConditions: '未达到优惠券使用门槛'
    }
  }
}

function resetTemplateForm() {
  Object.assign(templateForm, createDefaultForm())
}

function openCreateDrawer() {
  editingTemplateId.value = ''
  resetTemplateForm()
  formDrawerVisible.value = true
}

async function openEdit(row: CouponTemplate) {
  if (!row.id) {
    ElMessage.warning('当前优惠券模板缺少 ID，无法编辑')
    return
  }
  editingTemplateId.value = String(row.id)
  resetTemplateForm()
  formDrawerVisible.value = true
  const detail = await couponTemplateApi.detail(String(row.id))
  populateTemplateForm(detail)
}

async function loadTemplates() {
  loading.value = true
  try {
    const page = await couponTemplateApi.page({
      current: pagination.current,
      size: pagination.size,
      source: currentSource.value,
      name: filters.name || undefined,
      type: filters.type,
      target: filters.target,
      goods: filters.goods || undefined
    })
    templates.value = page.records || []
    pagination.total = Number(page.total || 0)
    pagination.current = Number(page.current || pagination.current)
    pagination.size = Number(page.size || pagination.size)
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.name = ''
  filters.type = undefined
  filters.target = undefined
  filters.goods = ''
  pagination.current = 1
  void loadTemplates()
}

function buildTemplatePayload(): CouponTemplateSavePayload {
  const receiveRule: ReceiveRuleDraft = {
    ...templateForm.receiveRule,
    distributionMode: authStore.isPlatform ? templateForm.receiveRule.distributionMode : undefined,
    applicableMerchantScope: authStore.isPlatform ? templateForm.receiveRule.applicableMerchantScope : 'SPECIFIED',
    applicableShopNumbers: authStore.isPlatform ? templateForm.receiveRule.applicableShopNumbers : authStore.shopNumber || ''
  }
  return {
    name: templateForm.name,
    source: currentSource.value,
    target: templateForm.target,
    goods: templateForm.target === 0 ? templateForm.goods : undefined,
    type: templateForm.type,
    validStartTime: templateForm.validRange[0],
    validEndTime: templateForm.validRange[1],
    stock: templateForm.stock,
    receiveRule: buildReceiveRule(receiveRule),
    consumeRule: buildConsumeRule(templateForm.type, templateForm.consumeRule)
  }
}

async function submitTemplate() {
  await templateFormRef.value?.validate()
  savingTemplate.value = true
  try {
    const payload = buildTemplatePayload()
    if (editingTemplateId.value) {
      await couponTemplateApi.update(editingTemplateId.value, payload)
      ElMessage.success('优惠券模板已更新')
    } else {
      await couponTemplateApi.create(payload)
      ElMessage.success('优惠券模板创建成功')
      pagination.current = 1
    }
    formDrawerVisible.value = false
    await loadTemplates()
  } finally {
    savingTemplate.value = false
  }
}

async function openDetail(row: CouponTemplate) {
  selectedTemplate.value = row
  detailDrawerVisible.value = true
  if (!row.id) {
    return
  }
  detailLoading.value = true
  try {
    selectedTemplate.value = await couponTemplateApi.detail(String(row.id))
  } finally {
    detailLoading.value = false
  }
}

function openIncrease(row: CouponTemplate) {
  increaseTarget.value = row
  increaseNumber.value = 100
  increaseDialogVisible.value = true
}

async function submitIncrease() {
  if (!increaseTarget.value?.id) {
    ElMessage.warning('当前优惠券模板缺少 ID，无法追加发行量')
    return
  }
  increasing.value = true
  try {
    await couponTemplateApi.increaseNumber({
      couponTemplateId: String(increaseTarget.value.id),
      number: increaseNumber.value
    })
    ElMessage.success('发行量已追加')
    increaseDialogVisible.value = false
    await loadTemplates()
  } finally {
    increasing.value = false
  }
}

async function terminateTemplate(row: CouponTemplate) {
  if (!row.id) {
    ElMessage.warning('当前优惠券模板缺少 ID，无法下架')
    return
  }
  await ElMessageBox.confirm(`确认下架“${row.name}”？下架后将不能继续发放。`, '下架优惠券模板', {
    confirmButtonText: '确认下架',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await couponTemplateApi.terminate(String(row.id))
  ElMessage.success('优惠券模板已下架')
  await loadTemplates()
}

function populateTemplateForm(template: CouponTemplate) {
  const receiveRule = parseJsonObject<Record<string, string | number>>(template.receiveRule)
  const consumeRule = parseJsonObject<Record<string, string | number>>(template.consumeRule)
  Object.assign(templateForm, {
    name: template.name,
    source: currentSource.value,
    target: template.target,
    goods: template.goods || '',
    type: template.type,
    validRange: [template.validStartTime, template.validEndTime].filter(Boolean),
    stock: template.stock,
    receiveRule: {
      limitPerPerson: Number(receiveRule.limitPerPerson || 1),
      usageInstructions: String(receiveRule.usageInstructions || ''),
      distributionMode: String(receiveRule.distributionMode || 'RECEIVE') as DistributionMode,
      applicableMerchantScope: String(receiveRule.applicableMerchantScope || 'ALL') as ApplicableMerchantScope,
      applicableShopNumbers: String(receiveRule.applicableShopNumbers || '')
    },
    consumeRule: {
      termsOfUse: Number(consumeRule.termsOfUse || consumeRule.thresholdAmount || 0),
      maximumDiscountAmount: Number(consumeRule.maximumDiscountAmount || consumeRule.discountAmount || consumeRule.maxDiscountAmount || 0),
      discountRate: consumeRule.discountRate ? Number(consumeRule.discountRate) : undefined,
      explanationOfUnmetConditions: String(consumeRule.explanationOfUnmetConditions || '未达到优惠券使用门槛')
    }
  })
}

onMounted(() => {
  void loadTemplates()
})
</script>
