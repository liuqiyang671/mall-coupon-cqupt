<template>
  <PermissionGate :roles="[2]">
    <section class="coupon-page">
      <div class="surface-card coupon-toolbar">
        <div>
          <p class="eyebrow">Coupon Center</p>
          <h3>领券中心</h3>
          <p class="muted">浏览邮惠券商城当前可领取的优惠券，查看门槛、有效期和库存后可直接领取到个人券包。</p>
        </div>
        <div class="toolbar-actions">
          <el-segmented v-model="redeemMode" :options="redeemModeOptions" />
          <el-button :icon="RefreshCw" @click="loadCoupons">刷新</el-button>
        </div>
      </div>

      <div class="surface-card filter-panel">
        <el-form :model="filters" label-position="top">
          <div class="filter-grid">
            <el-form-item label="店铺编号">
              <el-input v-model.trim="filters.shopNumber" placeholder="按店铺编号查询" clearable />
            </el-form-item>
            <el-form-item label="优惠券模板 ID">
              <el-input v-model.trim="filters.couponTemplateId" placeholder="按模板 ID 查询" clearable />
            </el-form-item>
            <el-form-item label="优惠券名称">
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
            <el-form-item label="券归属">
              <el-select v-model="filters.source" placeholder="全部归属" clearable>
                <el-option v-for="item in couponSourceOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </div>
          <div class="filter-actions">
            <el-button :icon="RefreshCw" @click="resetFilters">重置</el-button>
            <el-button type="primary" :icon="Search" @click="loadCoupons">查询</el-button>
          </div>
        </el-form>
      </div>

      <div class="coupon-center-grid" v-loading="loading">
        <article
          v-for="coupon in coupons"
          :key="coupon.id"
          class="surface-card coupon-center-card"
          :class="{ 'coupon-center-card--disabled': !isRedeemable(coupon) && !isProcessing(coupon) }"
        >
          <div class="coupon-center-card__main">
            <div class="coupon-center-card__amount">{{ formatCouponBenefit(coupon.type, coupon.consumeRule) }}</div>
            <div class="coupon-center-card__body">
              <div class="coupon-center-card__title-row">
                <h4>{{ coupon.name }}</h4>
                <el-tag :type="coupon.source === 1 ? 'warning' : 'success'">{{ getCouponSourceText(coupon.source) }}</el-tag>
              </div>
              <p>{{ getCouponTargetText(coupon.target) }}<span v-if="coupon.goods">：{{ coupon.goods }}</span></p>
              <div class="coupon-center-card__meta">
                <span>{{ formatReceiveLimit(coupon.receiveRule) }}</span>
                <span>{{ formatUsageInstructions(coupon.receiveRule) }}</span>
                <span>{{ formatApplicableMerchant(coupon.receiveRule) }}</span>
              </div>
              <div class="coupon-center-card__time">
                {{ coupon.validStartTime }} <span>至</span> {{ coupon.validEndTime }}
              </div>
            </div>
          </div>
          <div class="coupon-center-card__side">
            <el-tag :type="stockTagType(coupon)">{{ stockText(coupon) }}</el-tag>
            <el-button
              type="primary"
              :loading="isRedeeming(coupon)"
              :disabled="!canClickRedeem(coupon)"
              @click="redeemCoupon(coupon)"
            >
              {{ redeemButtonText(coupon) }}
            </el-button>
            <el-button link type="primary" :icon="Eye" @click="openDetail(coupon)">查看规则</el-button>
          </div>
        </article>

        <el-empty v-if="!loading && coupons.length === 0" description="暂无可领取优惠券" />
      </div>

      <div v-if="coupons.length" class="surface-card coupon-center-footer">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[8, 12, 24]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadCoupons"
          @current-change="loadCoupons"
        />
      </div>
    </section>

    <el-drawer v-model="detailDrawerVisible" title="优惠券规则" size="520px">
      <el-skeleton v-if="detailLoading" :rows="7" animated />
      <div v-else-if="selectedCoupon" class="detail-panel">
        <div class="coupon-preview-card">
          <span>{{ getCouponSourceText(selectedCoupon.source) }} · {{ getCouponTypeText(selectedCoupon.type) }}</span>
          <strong>{{ formatCouponBenefit(selectedCoupon.type, selectedCoupon.consumeRule) }}</strong>
          <p>{{ selectedCoupon.name }}</p>
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="模板 ID">{{ selectedCoupon.id || '-' }}</el-descriptions-item>
          <el-descriptions-item label="店铺编号">{{ selectedCoupon.shopNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="适用范围">
            {{ getCouponTargetText(selectedCoupon.target) }} {{ selectedCoupon.goods || '' }}
          </el-descriptions-item>
          <el-descriptions-item label="领取限制">{{ formatReceiveLimit(selectedCoupon.receiveRule) }}</el-descriptions-item>
          <el-descriptions-item label="使用说明">{{ formatUsageInstructions(selectedCoupon.receiveRule) }}</el-descriptions-item>
          <el-descriptions-item label="可用商家">{{ formatApplicableMerchant(selectedCoupon.receiveRule) }}</el-descriptions-item>
          <el-descriptions-item label="库存">{{ selectedCoupon.stock }}</el-descriptions-item>
          <el-descriptions-item label="有效期">{{ selectedCoupon.validStartTime }} 至 {{ selectedCoupon.validEndTime }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ availabilityText(selectedCoupon) }}</el-descriptions-item>
        </el-descriptions>
        <el-button
          type="primary"
          :loading="isRedeeming(selectedCoupon)"
          :disabled="!canClickRedeem(selectedCoupon)"
          @click="redeemCoupon(selectedCoupon)"
        >
          {{ redeemButtonText(selectedCoupon) }}
        </el-button>
      </div>
      <el-empty v-else description="未查询到优惠券" />
    </el-drawer>
  </PermissionGate>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Eye, RefreshCw, Search } from 'lucide-vue-next'
import { couponCenterApi } from '@/api/coupon'
import PermissionGate from '@/components/auth/PermissionGate.vue'
import type { CouponSource, CouponTarget, CouponTemplate, CouponType } from '@/types/coupon'
import {
  formatApplicableMerchant,
  formatCouponBenefit,
  formatReceiveLimit,
  getCouponSourceText,
  getCouponTargetText,
  getCouponTypeText,
  parseJsonObject
} from '@/utils/coupon'

type RedeemMode = 'sync' | 'mq'

const REDEEMED_STORAGE_KEY = 'youhuiquan.redeemed.template.ids'

const couponTypeOptions = [
  { label: '立减券', value: 0 },
  { label: '满减券', value: 1 },
  { label: '折扣券', value: 2 }
] as const

const couponTargetOptions = [
  { label: '商品专属', value: 0 },
  { label: '全店通用', value: 1 }
] as const

const couponSourceOptions = [
  { label: '店铺券', value: 0 },
  { label: '平台券', value: 1 }
] as const

const redeemModeOptions = [
  { label: '普通领取', value: 'sync' },
  { label: '高峰领取', value: 'mq' }
]

const loading = ref(false)
const detailLoading = ref(false)
const redeemMode = ref<RedeemMode>('sync')
const coupons = ref<CouponTemplate[]>([])
const selectedCoupon = ref<CouponTemplate | null>(null)
const detailDrawerVisible = ref(false)
const redeemingIds = ref<Set<string>>(new Set())
const processingIds = ref<Set<string>>(new Set())
const redeemedIds = ref<Set<string>>(readRedeemedIds())

const pagination = reactive({
  current: 1,
  size: 8,
  total: 0
})

const filters = reactive<{
  shopNumber: string
  couponTemplateId: string
  name: string
  source?: CouponSource
  target?: CouponTarget
  type?: CouponType
}>({
  shopNumber: '',
  couponTemplateId: '',
  name: '',
  source: undefined,
  target: undefined,
  type: undefined
})

const now = computed(() => Date.now())

function readRedeemedIds() {
  try {
    const raw = sessionStorage.getItem(REDEEMED_STORAGE_KEY)
    return new Set<string>(raw ? JSON.parse(raw) : [])
  } catch {
    return new Set<string>()
  }
}

function persistRedeemedIds() {
  sessionStorage.setItem(REDEEMED_STORAGE_KEY, JSON.stringify([...redeemedIds.value]))
}

async function loadCoupons() {
  loading.value = true
  try {
    const page = await couponCenterApi.page({
      current: pagination.current,
      size: pagination.size,
      shopNumber: filters.shopNumber || undefined,
      couponTemplateId: filters.couponTemplateId || undefined,
      name: filters.name || undefined,
      source: filters.source,
      target: filters.target,
      type: filters.type
    })
    coupons.value = page.records || []
    pagination.total = Number(page.total || 0)
    pagination.current = Number(page.current || pagination.current)
    pagination.size = Number(page.size || pagination.size)
  } catch (error) {
    const message = error instanceof Error ? error.message : '优惠券加载失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.shopNumber = ''
  filters.couponTemplateId = ''
  filters.name = ''
  filters.source = undefined
  filters.target = undefined
  filters.type = undefined
  pagination.current = 1
  void loadCoupons()
}

async function openDetail(coupon: CouponTemplate) {
  selectedCoupon.value = coupon
  detailDrawerVisible.value = true
  if (!coupon.id || !coupon.shopNumber) {
    return
  }
  detailLoading.value = true
  try {
    const detail = await couponCenterApi.detail({
      shopNumber: String(coupon.shopNumber),
      couponTemplateId: String(coupon.id)
    })
    selectedCoupon.value = {
      ...coupon,
      ...detail,
      shopNumber: detail.shopNumber || coupon.shopNumber
    }
  } finally {
    detailLoading.value = false
  }
}

async function redeemCoupon(coupon: CouponTemplate) {
  if (!coupon.id || !coupon.shopNumber) {
    ElMessage.warning('当前优惠券缺少模板 ID 或店铺编号，暂不能领取')
    return
  }
  const couponId = String(coupon.id)
  setId(redeemingIds.value, couponId, true)
  try {
    const payload = {
      source: 0,
      shopNumber: String(coupon.shopNumber),
      couponTemplateId: couponId
    }
    if (redeemMode.value === 'mq') {
      await couponCenterApi.redeemByMq(payload)
      setId(processingIds.value, couponId, true)
      ElMessage.success('领取请求已提交，请稍后在券包中查看')
    } else {
      await couponCenterApi.redeem(payload)
      markRedeemed(couponId)
      ElMessage.success('优惠券领取成功')
    }
    coupon.stock = Math.max(Number(coupon.stock || 0) - 1, 0)
  } catch (error) {
    const message = error instanceof Error ? error.message : '领取失败，请稍后再试'
    if (message.includes('领取上限') || message.includes('重复领取')) {
      markRedeemed(couponId)
    }
    if (message.includes('领取完')) {
      coupon.stock = 0
    }
    ElMessage.error(message)
  } finally {
    setId(redeemingIds.value, couponId, false)
  }
}

function markRedeemed(couponId: string) {
  setId(redeemedIds.value, couponId, true)
  setId(processingIds.value, couponId, false)
  persistRedeemedIds()
}

function setId(target: Set<string>, id: string, enabled: boolean) {
  enabled ? target.add(id) : target.delete(id)
  if (target === redeemedIds.value) {
    redeemedIds.value = new Set(target)
  }
  if (target === redeemingIds.value) {
    redeemingIds.value = new Set(target)
  }
  if (target === processingIds.value) {
    processingIds.value = new Set(target)
  }
}

function couponId(coupon: CouponTemplate) {
  return coupon.id ? String(coupon.id) : ''
}

function isRedeeming(coupon: CouponTemplate) {
  return redeemingIds.value.has(couponId(coupon))
}

function isProcessing(coupon: CouponTemplate) {
  return processingIds.value.has(couponId(coupon))
}

function isRedeemed(coupon: CouponTemplate) {
  return redeemedIds.value.has(couponId(coupon))
}

function isRedeemable(coupon: CouponTemplate) {
  return coupon.status !== 1 && Number(coupon.stock || 0) > 0 && now.value >= timeOf(coupon.validStartTime) && now.value <= timeOf(coupon.validEndTime)
}

function canClickRedeem(coupon: CouponTemplate) {
  return isRedeemable(coupon) && !isRedeemed(coupon) && !isProcessing(coupon)
}

function redeemButtonText(coupon: CouponTemplate) {
  if (isProcessing(coupon)) return '处理中'
  if (isRedeemed(coupon)) return '已领取'
  if (Number(coupon.stock || 0) <= 0) return '已抢光'
  if (now.value < timeOf(coupon.validStartTime)) return '未开始'
  if (now.value > timeOf(coupon.validEndTime) || coupon.status === 1) return '已结束'
  return '立即领取'
}

function availabilityText(coupon: CouponTemplate) {
  if (Number(coupon.stock || 0) <= 0) return '已抢光'
  if (now.value < timeOf(coupon.validStartTime)) return '未开始'
  if (now.value > timeOf(coupon.validEndTime) || coupon.status === 1) return '已结束'
  return '可领取'
}

function stockText(coupon: CouponTemplate) {
  const stock = Number(coupon.stock || 0)
  if (stock <= 0) return '库存不足'
  if (stock <= 20) return `仅剩 ${stock} 张`
  return `剩余 ${stock} 张`
}

function stockTagType(coupon: CouponTemplate) {
  const stock = Number(coupon.stock || 0)
  if (stock <= 0) return 'danger'
  if (stock <= 20) return 'warning'
  return 'success'
}

function timeOf(value?: string) {
  return value ? new Date(value.replace(/-/g, '/')).getTime() : 0
}

function formatUsageInstructions(receiveRule?: string) {
  const rule = parseJsonObject<Record<string, string | number>>(receiveRule)
  return String(rule.usageInstructions || '按页面展示规则使用')
}

onMounted(() => {
  void loadCoupons()
})
</script>

<style scoped>
.coupon-center-grid {
  display: grid;
  gap: 14px;
  min-height: 220px;
}

.coupon-center-card {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 156px;
  gap: 18px;
  overflow: hidden;
  padding: 18px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(247, 254, 255, 0.9));
}

.coupon-center-card::before {
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  background: var(--gradient-brand);
  content: "";
}

.coupon-center-card:hover {
  transform: translateY(-2px);
}

.coupon-center-card--disabled {
  opacity: 0.72;
}

.coupon-center-card__main {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 18px;
  min-width: 0;
}

.coupon-center-card__amount {
  display: grid;
  min-height: 132px;
  place-items: center;
  padding: 18px;
  border-radius: 8px;
  color: #fff;
  background:
    radial-gradient(circle at 18% 18%, rgba(255, 255, 255, 0.34), transparent 24%),
    var(--gradient-coupon-fixed);
  box-shadow: 0 18px 36px rgba(255, 90, 95, 0.18);
  font-family: var(--font-family-number);
  font-size: 26px;
  font-weight: 900;
  text-align: center;
}

.coupon-center-card__body {
  min-width: 0;
}

.coupon-center-card__title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.coupon-center-card h4 {
  margin: 0;
  font-size: 18px;
  line-height: 1.4;
}

.coupon-center-card p {
  margin: 8px 0;
  color: var(--color-muted);
}

.coupon-center-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.coupon-center-card__meta span {
  padding: 5px 8px;
  border: 1px solid rgba(0, 167, 199, 0.18);
  border-radius: 8px;
  color: var(--color-brand-dark);
  background: var(--color-brand-soft);
  font-size: 12px;
}

.coupon-center-card__time {
  margin-top: 12px;
  color: var(--color-muted);
  font-size: 13px;
}

.coupon-center-card__time span {
  margin: 0 6px;
}

.coupon-center-card__side {
  display: grid;
  align-content: center;
  gap: 10px;
}

.coupon-center-card__side .el-button {
  width: 100%;
}

.coupon-center-footer {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
}

@media (max-width: 900px) {
  .coupon-center-card,
  .coupon-center-card__main {
    grid-template-columns: 1fr;
  }

  .coupon-center-card__amount {
    min-height: 96px;
  }
}
</style>
