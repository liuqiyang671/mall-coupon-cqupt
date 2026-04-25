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
              {{ remind.remindType[index] || '提醒' }} · {{ time }}
            </el-tag>
          </div>
        </article>

        <el-empty v-if="!loading && reminds.length === 0" description="暂无预约提醒">
          <el-button type="primary" @click="openCreateDialog">新建提醒</el-button>
        </el-empty>
      </div>
    </section>

    <el-dialog v-model="createVisible" title="新建开抢提醒" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="two-column-form">
          <el-form-item label="优惠券模板 ID" prop="couponTemplateId">
            <el-input v-model.trim="form.couponTemplateId" placeholder="请输入模板 ID" clearable />
          </el-form-item>
          <el-form-item label="店铺编号" prop="shopNumber">
            <el-input v-model.trim="form.shopNumber" placeholder="请输入店铺编号" clearable />
          </el-form-item>
        </div>
        <el-form-item label="优惠券名称">
          <el-input v-model.trim="form.name" placeholder="用于提醒列表展示，可选" clearable />
        </el-form-item>
        <el-form-item label="开抢时间" prop="startTime">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择优惠券开始领取时间"
            style="width: 100%"
          />
        </el-form-item>
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { BellPlus, RefreshCw } from 'lucide-vue-next'
import { couponRemindApi } from '@/api/coupon'
import PermissionGate from '@/components/auth/PermissionGate.vue'
import { useAuthStore } from '@/stores/auth'
import type { CouponRemind, CouponRemindCreatePayload, CouponType } from '@/types/coupon'
import { formatReceiveLimit, getCouponTargetText, getCouponTypeText } from '@/utils/coupon'

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
const createVisible = ref(false)
const reminds = ref<CouponRemind[]>([])
const formRef = ref<FormInstance>()
const form = reactive<RemindForm>(createDefaultForm())

const remindTimeOptions = [
  { label: '提前 5 分钟', value: 5 },
  { label: '提前 10 分钟', value: 10 },
  { label: '提前 15 分钟', value: 15 },
  { label: '提前 30 分钟', value: 30 },
  { label: '提前 60 分钟', value: 60 }
]

const rules: FormRules<RemindForm> = {
  couponTemplateId: [{ required: true, message: '请输入优惠券模板 ID', trigger: 'blur' }],
  shopNumber: [{ required: true, message: '请输入店铺编号', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开抢时间', trigger: 'change' }],
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

function openCreateDialog() {
  Object.assign(form, createDefaultForm())
  createVisible.value = true
}

async function submitRemind() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const payload: CouponRemindCreatePayload = {
      couponTemplateId: form.couponTemplateId,
      shopNumber: form.shopNumber,
      userId: authStore.userId,
      name: form.name || undefined,
      contact: form.contact,
      type: form.type,
      remindTime: form.remindTime,
      startTime: form.startTime
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
  return label?.includes('短信') ? 1 : 0
}

function timeOf(value?: string) {
  return value ? new Date(value.replace(/-/g, '/')).getTime() : 0
}

function couponType(remind: CouponRemind): CouponType {
  return (remind.type ?? 1) as CouponType
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

@media (max-width: 900px) {
  .remind-card {
    grid-template-columns: 1fr;
  }
}
</style>
