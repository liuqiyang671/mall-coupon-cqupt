<template>
  <PermissionGate :roles="[2]">
    <section class="coupon-page">
      <div class="surface-card coupon-toolbar">
        <div>
          <p class="eyebrow">My Coupons</p>
          <h3>我的优惠券</h3>
          <p class="muted">查看已领取优惠券，按状态管理可用券、锁定券、已使用券和过期券。</p>
        </div>
        <div class="toolbar-actions">
          <el-button :icon="Ticket" @click="router.push({ name: 'coupon-center' })">去领券</el-button>
          <el-button :icon="RefreshCw" @click="loadCoupons">刷新</el-button>
        </div>
      </div>

      <div class="surface-card filter-panel coupon-tabs-panel">
        <el-segmented v-model="activeStatus" :options="statusOptions" @change="handleStatusChange" />
      </div>

      <div class="coupon-wallet-list" v-loading="loading">
        <article
          v-for="coupon in coupons"
          :key="coupon.id"
          class="surface-card wallet-coupon-card"
          :class="{ 'wallet-coupon-card--muted': coupon.status !== 0 }"
        >
          <div class="wallet-coupon-card__amount">{{ formatCouponValue(coupon) }}</div>
          <div class="wallet-coupon-card__content">
            <div class="wallet-coupon-card__title">
              <h4>{{ coupon.name || `优惠券 ${coupon.couponTemplateId}` }}</h4>
              <el-tag :type="userCouponStatusTagType(coupon.status)">{{ getUserCouponStatusText(coupon.status) }}</el-tag>
            </div>
            <p>
              {{ getCouponTargetText(coupon.target) }}
              <span v-if="coupon.goods">：{{ coupon.goods }}</span>
              <span v-if="coupon.shopNumber"> · 店铺 {{ coupon.shopNumber }}</span>
            </p>
            <div class="wallet-coupon-card__meta">
              <span>{{ formatReceiveLimit(coupon.receiveRule) }}</span>
              <span>{{ formatUsageInstructions(coupon.receiveRule) }}</span>
              <span>领取次数 {{ coupon.receiveCount || 1 }}</span>
            </div>
            <div class="wallet-coupon-card__time">
              有效期：{{ coupon.validStartTime || '-' }} 至 {{ coupon.validEndTime || '-' }}
            </div>
          </div>
          <div class="wallet-coupon-card__actions">
            <el-button type="primary" :disabled="coupon.status !== 0" @click="goSettlement(coupon)">去使用</el-button>
            <el-button link type="primary" :icon="Info" @click="openDetail(coupon)">详情</el-button>
          </div>
        </article>

        <el-empty v-if="!loading && coupons.length === 0" description="当前状态暂无优惠券">
          <el-button type="primary" @click="router.push({ name: 'coupon-center' })">去领券</el-button>
        </el-empty>
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

    <el-drawer v-model="detailVisible" title="优惠券详情" size="520px">
      <div v-if="selectedCoupon" class="detail-panel">
        <div class="coupon-preview-card">
          <span>{{ getUserCouponStatusText(selectedCoupon.status) }} · {{ getCouponTypeText(couponType(selectedCoupon)) }}</span>
          <strong>{{ formatCouponValue(selectedCoupon) }}</strong>
          <p>{{ selectedCoupon.name || selectedCoupon.couponTemplateId }}</p>
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="用户券 ID">{{ selectedCoupon.id }}</el-descriptions-item>
          <el-descriptions-item label="模板 ID">{{ selectedCoupon.couponTemplateId }}</el-descriptions-item>
          <el-descriptions-item label="店铺编号">{{ selectedCoupon.shopNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="适用范围">
            {{ getCouponTargetText(selectedCoupon.target) }} {{ selectedCoupon.goods || '' }}
          </el-descriptions-item>
          <el-descriptions-item label="领取规则">{{ formatReceiveLimit(selectedCoupon.receiveRule) }}</el-descriptions-item>
          <el-descriptions-item label="使用说明">{{ formatUsageInstructions(selectedCoupon.receiveRule) }}</el-descriptions-item>
          <el-descriptions-item label="领取时间">{{ selectedCoupon.receiveTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="有效期">{{ selectedCoupon.validStartTime || '-' }} 至 {{ selectedCoupon.validEndTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="使用时间">{{ selectedCoupon.useTime || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <el-empty v-else description="未选择优惠券" />
    </el-drawer>
  </PermissionGate>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Info, RefreshCw, Ticket } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { userCouponApi } from '@/api/coupon'
import PermissionGate from '@/components/auth/PermissionGate.vue'
import type { CouponType, UserCoupon, UserCouponStatus } from '@/types/coupon'
import {
  formatCouponBenefit,
  formatReceiveLimit,
  formatUsageInstructions,
  getCouponTargetText,
  getCouponTypeText,
  getUserCouponStatusText,
  userCouponStatusTagType
} from '@/utils/coupon'

const router = useRouter()

const statusOptions: Array<{ label: string; value: UserCouponStatus }> = [
  { label: '未使用', value: 0 },
  { label: '已锁定', value: 1 },
  { label: '已使用', value: 2 },
  { label: '已过期', value: 3 },
  { label: '已撤回', value: 4 }
]

const activeStatus = ref<UserCouponStatus>(0)
const loading = ref(false)
const coupons = ref<UserCoupon[]>([])
const detailVisible = ref(false)
const selectedCoupon = ref<UserCoupon | null>(null)

const pagination = reactive({
  current: 1,
  size: 8,
  total: 0
})

async function loadCoupons() {
  loading.value = true
  try {
    const page = await userCouponApi.page({
      current: pagination.current,
      size: pagination.size,
      status: activeStatus.value
    })
    coupons.value = page.records || []
    pagination.total = Number(page.total || 0)
    pagination.current = Number(page.current || pagination.current)
    pagination.size = Number(page.size || pagination.size)
  } finally {
    loading.value = false
  }
}

function handleStatusChange() {
  pagination.current = 1
  void loadCoupons()
}

function couponType(coupon: UserCoupon): CouponType {
  return (coupon.type ?? 1) as CouponType
}

function formatCouponValue(coupon: UserCoupon) {
  return coupon.consumeRule ? formatCouponBenefit(couponType(coupon), coupon.consumeRule) : '优惠券'
}

function openDetail(coupon: UserCoupon) {
  selectedCoupon.value = coupon
  detailVisible.value = true
}

function goSettlement(coupon: UserCoupon) {
  void router.push({
    name: 'settlement',
    query: {
      couponId: coupon.id,
      shopNumber: coupon.shopNumber || ''
    }
  })
}

onMounted(() => {
  void loadCoupons()
})
</script>

<style scoped>
.coupon-tabs-panel {
  overflow-x: auto;
}

.coupon-wallet-list {
  display: grid;
  gap: 14px;
  min-height: 220px;
}

.wallet-coupon-card {
  position: relative;
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr) 132px;
  gap: 18px;
  overflow: hidden;
  padding: 18px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(247, 254, 255, 0.9));
}

.wallet-coupon-card::before {
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  background: var(--gradient-brand);
  content: "";
}

.wallet-coupon-card:hover {
  transform: translateY(-2px);
}

.wallet-coupon-card--muted {
  opacity: 0.72;
}

.wallet-coupon-card__amount {
  display: grid;
  min-height: 126px;
  place-items: center;
  padding: 18px;
  border-radius: 8px;
  color: #fff;
  background:
    radial-gradient(circle at 18% 18%, rgba(255, 255, 255, 0.34), transparent 24%),
    var(--gradient-coupon-fixed);
  box-shadow: 0 18px 36px rgba(255, 90, 95, 0.18);
  font-family: var(--font-family-number);
  font-size: 24px;
  font-weight: 900;
  text-align: center;
}

.wallet-coupon-card--muted .wallet-coupon-card__amount {
  background: linear-gradient(135deg, #6b7280, #9ca3af);
}

.wallet-coupon-card__title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.wallet-coupon-card h4 {
  margin: 0;
  font-size: 18px;
  line-height: 1.4;
}

.wallet-coupon-card p,
.wallet-coupon-card__time {
  color: var(--color-muted);
}

.wallet-coupon-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.wallet-coupon-card__meta span {
  padding: 5px 8px;
  border: 1px solid rgba(0, 167, 199, 0.18);
  border-radius: 8px;
  color: var(--color-brand-dark);
  background: var(--color-brand-soft);
  font-size: 12px;
}

.wallet-coupon-card__time {
  margin-top: 12px;
  font-size: 13px;
}

.wallet-coupon-card__actions {
  display: grid;
  align-content: center;
  gap: 10px;
}

.wallet-coupon-card__actions .el-button {
  width: 100%;
}

.coupon-center-footer {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
}

@media (max-width: 900px) {
  .wallet-coupon-card {
    grid-template-columns: 1fr;
  }

  .wallet-coupon-card__amount {
    min-height: 96px;
  }
}
</style>
