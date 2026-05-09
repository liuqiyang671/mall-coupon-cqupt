<template>
  <PermissionGate :roles="[2]">
    <section class="coupon-page">
      <div class="surface-card coupon-toolbar">
        <div>
          <p class="eyebrow">Coupon Remind</p>
          <h3>预约提醒</h3>
          <p class="muted">管理优惠券开抢提醒，可按邮件或短信方式在活动开始前收到通知。</p>
        </div>
        <div class="toolbar-actions">
          <el-button :icon="RefreshCw" @click="loadReminds">刷新</el-button>
          <el-button type="primary" :icon="BellPlus" @click="openCreateDialog">新建提醒</el-button>
        </div>
      </div>

      <div class="remind-list" v-loading="loading">
        <article v-for="remind in reminds" :key="remind.id" class="surface-card remind-card">
          <div>
            <div class="remind-card__title">
              <h4>{{ remind.name || `优惠券 ${remind.id}` }}</h4>
              <el-tag>{{ getCouponTypeText(couponType(remind)) }}</el-tag>
            </div>
            <p>
              店铺 {{ remind.shopNumber }} · {{ getCouponTargetText(remind.target) }}
              <span v-if="remind.goods">：{{ remind.goods }}</span>
            </p>
            <div class="remind-card__meta">
              <span>开抢时间：{{ remind.validStartTime }}</span>
              <span>结束时间：{{ remind.validEndTime }}</span>
              <span>{{ formatReceiveLimit(remind.receiveRule) }}</span>
            </div>
          </div>
          <div class="remind-card__times">
            <el-tag
              v-for="(time, index) in remind.remindTime"
              :key="`${remind.id}-${time}-${index}`"
              closable
              @close="cancelRemind(remind, index)"
            >
              {{ formatRemindTypeLabel(remind.remindType[index]) }} · {{ formatDateTime(time) }}
            </el-tag>
          </div>
        </article>

        <el-empty v-if="!loading && reminds.length === 0" description="暂无预约提醒">
          <el-button type="primary" @click="openCreateDialog">新建提醒</el-button>
        </el-empty>
      </div>
    </section>

    <el-dialog v-model="createVisible" title="新建开抢提醒" width="760px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="选择优惠券" prop="couponTemplateId">
          <div class="coupon-picker">
            <div class="coupon-picker__filters">
              <el-input v-model.trim="couponFilters.name" placeholder="搜索优惠券名称" clearable @keyup.enter="searchCandidateCoupons" />
              <el-input v-model.trim="couponFilters.shopNumber" placeholder="店铺编号" clearable @keyup.enter="searchCandidateCoupons" />
              <el-select v-model="couponFilters.source" placeholder="券归属" clearable>
                <el-option v-for="item in couponSourceOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
              <el-button :icon="Search" @click="searchCandidateCoupons">查询</el-button>
            </div>

            <div v-loading="couponLoading" class="coupon-picker__list">
              <button
                v-for="coupon in candidateCoupons"
                :key="couponKey(coupon)"
                class="coupon-picker__item"
                :class="{
                  'is-active': selectedCouponKey === couponKey(coupon),
                  'is-disabled': !canRemindCoupon(coupon)
                }"
                type="button"
                @click="selectCoupon(coupon)"
              >
                <span class="coupon-picker__benefit">{{ formatCouponBenefit(couponTemplateType(coupon), coupon.consumeRule) }}</span>
                <span class="coupon-picker__body">
                  <span class="coupon-picker__title">
                    <strong>{{ coupon.name }}</strong>
                    <el-tag :type="coupon.source === 1 ? 'warning' : 'success'" size="small">
                      {{ getCouponSourceText(coupon.source) }}
                    </el-tag>
                  </span>
                  <span class="coupon-picker__meta">
                    店铺 {{ coupon.shopNumber || '-' }} · {{ getCouponTargetText(coupon.target) }}
                    <template v-if="coupon.goods">：{{ coupon.goods }}</template>
                  </span>
                  <span class="coupon-picker__chips">
                    <el-tag size="small" effect="plain">{{ formatReceiveLimit(coupon.receiveRule) }}</el-tag>
                    <el-tag size="small" effect="plain">{{ coupon.validStartTime }} 开抢</el-tag>
                  </span>
                </span>
                <el-tag :type="remindAvailabilityTagType(coupon)" size="small">
                  {{ remindAvailabilityText(coupon) }}
                </el-tag>
              </button>

              <el-empty v-if="!couponLoading && candidateCoupons.length === 0" description="暂无可预约优惠券" />
            </div>

            <div v-if="candidateCoupons.length" class="coupon-picker__footer">
              <el-pagination
                v-model:current-page="couponPagination.current"
                small
                background
                :page-size="couponPagination.size"
                :total="couponPagination.total"
                layout="prev, pager, next"
                @current-change="loadCandidateCoupons"
              />
            </div>
          </div>
        </el-form-item>
        <div v-if="selectedCoupon" class="selected-coupon-summary">
          <span>已选：{{ selectedCoupon.name }}</span>
          <strong>{{ selectedCoupon.validStartTime }} 开抢</strong>
        </div>
        <div class="two-column-form">
          <el-form-item label="提前提醒" prop="remindTime">
            <el-select v-model="form.remindTime">
              <el-option v-for="item in remindTimeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="提醒方式" prop="type">
            <el-radio-group v-model="form.type">
              <el-radio-button :label="0">邮件</el-radio-button>
              <el-radio-button :label="1">短信</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </div>
        <el-form-item label="联系方式" prop="contact">
          <el-input v-model.trim="form.contact" placeholder="邮箱或手机号" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitRemind">提交提醒</el-button>
      </template>
    </el-dialog>
  </PermissionGate>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { BellPlus, RefreshCw, Search } from 'lucide-vue-next'
import { couponCenterApi, couponRemindApi } from '@/api/coupon'
import PermissionGate from '@/components/auth/PermissionGate.vue'
import { useAuthStore } from '@/stores/auth'
import type { CouponRemind, CouponRemindCreatePayload, CouponSource, CouponTemplate, CouponType } from '@/types/coupon'
import { formatCouponBenefit, formatReceiveLimit, getCouponSourceText, getCouponTargetText, getCouponTypeText } from '@/utils/coupon'

interface RemindForm {
  couponTemplateId: string
  shopNumber: string
  name: string
  startTime: string
  remindTime: number
  type: number
  contact: string
}

const authStore = useAuthStore()

const loading = ref(false)
const submitting = ref(false)
const couponLoading = ref(false)
const createVisible = ref(false)
const reminds = ref<CouponRemind[]>([])
const candidateCoupons = ref<CouponTemplate[]>([])
const selectedCoupon = ref<CouponTemplate | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<RemindForm>(createDefaultForm())

const selectedCouponKey = computed(() => (selectedCoupon.value ? couponKey(selectedCoupon.value) : ''))

const couponPagination = reactive({
  current: 1,
  size: 5,
  total: 0
})

const couponFilters = reactive<{
  name: string
  shopNumber: string
  source?: CouponSource
}>({
  name: '',
  shopNumber: '',
  source: undefined
})

const remindTimeOptions = [
  { label: '提前 5 分钟', value: 5 },
  { label: '提前 10 分钟', value: 10 },
  { label: '提前 15 分钟', value: 15 },
  { label: '提前 30 分钟', value: 30 },
  { label: '提前 60 分钟', value: 60 }
]

const couponSourceOptions = [
  { label: '店铺券', value: 0 },
  { label: '平台券', value: 1 }
] as const

const rules: FormRules<RemindForm> = {
  couponTemplateId: [{ required: true, message: '请选择要提醒的优惠券', trigger: 'change' }],
  remindTime: [{ required: true, message: '请选择提前提醒时间', trigger: 'change' }],
  type: [{ required: true, message: '请选择提醒方式', trigger: 'change' }],
  contact: [
    {
      validator: (_rule, value, callback) => {
        if (!value) {
          callback(new Error('请输入联系方式'))
          return
        }
        const emailOk = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)
        const phoneOk = /^1[3-9]\d{9}$/.test(value)
        emailOk || phoneOk ? callback() : callback(new Error('请输入正确手机号或邮箱'))
      },
      trigger: 'blur'
    }
  ]
}

function createDefaultForm(): RemindForm {
  return {
    couponTemplateId: '',
    shopNumber: '',
    name: '',
    startTime: '',
    remindTime: 5,
    type: 0,
    contact: ''
  }
}

async function loadReminds() {
  loading.value = true
  try {
    reminds.value = await couponRemindApi.list({
      userId: authStore.userId,
      current: 1,
      size: 100
    })
  } finally {
    loading.value = false
  }
}

async function loadCandidateCoupons() {
  couponLoading.value = true
  try {
    const page = await couponCenterApi.page({
      current: couponPagination.current,
      size: couponPagination.size,
      name: couponFilters.name || undefined,
      shopNumber: couponFilters.shopNumber || undefined,
      source: couponFilters.source,
      remindFirst: true
    })
    candidateCoupons.value = page.records || []
    couponPagination.total = Number(page.total || 0)
    couponPagination.current = Number(page.current || couponPagination.current)
    couponPagination.size = Number(page.size || couponPagination.size)
  } finally {
    couponLoading.value = false
  }
}

function searchCandidateCoupons() {
  couponPagination.current = 1
  void loadCandidateCoupons()
}

function openCreateDialog() {
  Object.assign(form, createDefaultForm())
  selectedCoupon.value = null
  couponFilters.name = ''
  couponFilters.shopNumber = ''
  couponFilters.source = undefined
  couponPagination.current = 1
  createVisible.value = true
  void loadCandidateCoupons()
  setTimeout(() => formRef.value?.clearValidate(), 0)
}

function selectCoupon(coupon: CouponTemplate) {
  if (!canRemindCoupon(coupon)) {
    ElMessage.warning(remindAvailabilityText(coupon) === '已开抢' ? '该优惠券已经开始领取，无法创建开抢前提醒' : '该优惠券暂不可预约提醒')
    return
  }
  selectedCoupon.value = coupon
  form.couponTemplateId = String(coupon.id)
  form.shopNumber = String(coupon.shopNumber)
  form.name = coupon.name
  form.startTime = coupon.validStartTime
  void formRef.value?.validateField('couponTemplateId')
}

async function submitRemind() {
  await formRef.value?.validate()
  if (!selectedCoupon.value || !canRemindCoupon(selectedCoupon.value)) {
    ElMessage.warning('请选择一张可预约的优惠券')
    return
  }
  submitting.value = true
  try {
    const coupon = selectedCoupon.value
    const payload: CouponRemindCreatePayload = {
      couponTemplateId: String(coupon.id),
      shopNumber: String(coupon.shopNumber),
      userId: authStore.userId,
      name: coupon.name || undefined,
      contact: form.contact,
      type: form.type,
      remindTime: form.remindTime,
      startTime: coupon.validStartTime
    }
    await couponRemindApi.create(payload)
    ElMessage.success('预约提醒已创建')
    createVisible.value = false
    await loadReminds()
  } finally {
    submitting.value = false
  }
}

async function cancelRemind(remind: CouponRemind, index: number) {
  const remindMinutes = getRemindMinutes(remind, index)
  const remindType = getRemindType(remind.remindType[index])
  await ElMessageBox.confirm('确认取消该预约提醒？', '取消提醒', {
    confirmButtonText: '取消提醒',
    cancelButtonText: '返回',
    type: 'warning'
  })
  await couponRemindApi.cancel({
    couponTemplateId: String(remind.id),
    userId: authStore.userId,
    remindTime: remindMinutes,
    type: remindType
  })
  ElMessage.success('预约提醒已取消')
  await loadReminds()
}

function getRemindMinutes(remind: CouponRemind, index: number) {
  const start = timeOf(remind.validStartTime)
  const remindTime = timeOf(remind.remindTime[index])
  const minutes = Math.round((start - remindTime) / 60000)
  return Number.isFinite(minutes) && minutes > 0 ? minutes : 5
}

function getRemindType(label?: string) {
  if (!label) return 0
  return label.includes('短信') || label.includes('鐭俊') || label.toLowerCase().includes('message') ? 1 : 0
}

function formatRemindTypeLabel(label?: string) {
  if (!label) return '提醒'
  if (label.includes('短信') || label.includes('鐭俊') || label.toLowerCase().includes('message')) return '短信提醒'
  if (label.includes('邮件') || label.includes('閭欢') || label.toLowerCase().includes('email')) return '邮件提醒'
  return label
}

function formatDateTime(value?: string) {
  if (!value) return '-'
  const timestamp = timeOf(value)
  if (!Number.isFinite(timestamp) || timestamp <= 0) return value
  const date = new Date(timestamp)
  const pad = (num: number) => String(num).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function timeOf(value?: string) {
  return value ? new Date(value.replace(/-/g, '/')).getTime() : 0
}

function couponType(remind: CouponRemind): CouponType {
  return (remind.type ?? 1) as CouponType
}

function couponTemplateType(coupon: CouponTemplate): CouponType {
  return (coupon.type ?? 1) as CouponType
}

function couponKey(coupon: CouponTemplate) {
  return `${coupon.shopNumber || 'platform'}-${coupon.id || coupon.name}`
}

function canRemindCoupon(coupon: CouponTemplate) {
  return Boolean(coupon.id && coupon.shopNumber)
    && coupon.status !== 1
    && Number(coupon.stock || 0) > 0
    && timeOf(coupon.validStartTime) > Date.now()
    && timeOf(coupon.validEndTime) > Date.now()
}

function remindAvailabilityText(coupon: CouponTemplate) {
  if (!coupon.id || !coupon.shopNumber) return '信息不完整'
  if (coupon.status === 1 || timeOf(coupon.validEndTime) <= Date.now()) return '已结束'
  if (Number(coupon.stock || 0) <= 0) return '已抢光'
  if (timeOf(coupon.validStartTime) <= Date.now()) return '已开抢'
  return '可预约'
}

function remindAvailabilityTagType(coupon: CouponTemplate) {
  const text = remindAvailabilityText(coupon)
  if (text === '可预约') return 'success'
  if (text === '已开抢') return 'warning'
  if (text === '已抢光' || text === '已结束') return 'danger'
  return 'info'
}

onMounted(() => {
  void loadReminds()
})
</script>

<style scoped>
.remind-list {
  display: grid;
  gap: 14px;
  min-height: 220px;
}

.remind-card {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(220px, 0.4fr);
  gap: 18px;
  overflow: hidden;
  padding: 20px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(247, 254, 255, 0.9));
}

.remind-card::before {
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  background: var(--gradient-brand);
  content: "";
}

.remind-card:hover {
  transform: translateY(-2px);
}

.remind-card__title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.remind-card h4 {
  margin: 0;
  font-size: 18px;
}

.remind-card p {
  color: var(--color-muted);
}

.remind-card__meta,
.remind-card__times {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.remind-card__meta span {
  padding: 5px 8px;
  border: 1px solid rgba(0, 167, 199, 0.18);
  border-radius: 8px;
  color: var(--color-brand-dark);
  background: var(--color-brand-soft);
  font-size: 12px;
}

.remind-card__times {
  align-content: center;
}

.coupon-picker {
  display: grid;
  gap: 12px;
  width: 100%;
}

.coupon-picker__filters {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) 150px 130px auto;
  gap: 10px;
  align-items: center;
}

.coupon-picker__list {
  display: grid;
  gap: 10px;
  min-height: 220px;
}

.coupon-picker__item {
  display: grid;
  grid-template-columns: 128px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  width: 100%;
  padding: 12px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  color: inherit;
  background: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  text-align: left;
  transition:
    border-color var(--duration-normal) var(--ease-out),
    box-shadow var(--duration-normal) var(--ease-out),
    transform var(--duration-normal) var(--ease-out);
}

.coupon-picker__item:hover {
  transform: translateY(-1px);
  border-color: rgba(154, 27, 61, 0.28);
  box-shadow: var(--shadow-md);
}

.coupon-picker__item.is-active {
  border-color: var(--color-brand);
  box-shadow: 0 0 0 3px rgba(154, 27, 61, 0.1);
}

.coupon-picker__item.is-disabled {
  opacity: 0.62;
}

.coupon-picker__benefit {
  display: grid;
  min-height: 74px;
  place-items: center;
  padding: 12px;
  border-radius: 8px;
  color: #fff;
  background: var(--gradient-coupon-fixed);
  font-family: var(--font-family-number);
  font-size: 18px;
  font-weight: 900;
  text-align: center;
}

.coupon-picker__body,
.coupon-picker__title,
.coupon-picker__chips {
  display: flex;
}

.coupon-picker__body {
  flex-direction: column;
  gap: 7px;
  min-width: 0;
}

.coupon-picker__title {
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.coupon-picker__title strong,
.selected-coupon-summary span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.coupon-picker__meta {
  color: var(--color-muted);
  font-size: 13px;
}

.coupon-picker__chips {
  flex-wrap: wrap;
  gap: 6px;
}

.coupon-picker__footer {
  display: flex;
  justify-content: flex-end;
}

.selected-coupon-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
  padding: 10px 12px;
  border: 1px solid rgba(154, 27, 61, 0.14);
  border-radius: 8px;
  color: var(--color-brand-dark);
  background: rgba(255, 245, 247, 0.82);
  font-size: 13px;
}

.selected-coupon-summary strong {
  flex: 0 0 auto;
  color: var(--color-brand);
}

@media (max-width: 900px) {
  .remind-card,
  .coupon-picker__filters,
  .coupon-picker__item {
    grid-template-columns: 1fr;
  }

  .selected-coupon-summary {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
