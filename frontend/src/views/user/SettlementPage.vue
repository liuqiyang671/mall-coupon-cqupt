<template>
  <PermissionGate :roles="[2]">
    <section class="coupon-page">
      <div class="surface-card coupon-toolbar">
        <div>
          <p class="eyebrow">Settlement</p>
          <h3>优惠券结算</h3>
          <p class="muted">订单确认页，根据购物车商品、订单金额和商品明细查询可用优惠券并应用到订单。</p>
        </div>
        <div class="toolbar-actions">
          <el-button :icon="Ticket" @click="drawerVisible = true">选择优惠券</el-button>
          <el-button type="primary" :icon="RefreshCw" @click="queryCoupons">重新计算</el-button>
        </div>
      </div>

      <div class="settlement-grid" data-testid="settlement-page">
        <article class="surface-card settlement-panel">
          <div class="section-heading">
            <div>
              <p class="eyebrow">Order</p>
              <h3>订单信息</h3>
            </div>
          </div>
          <el-form :model="orderForm" label-position="top">
            <div class="two-column-form">
              <el-form-item label="订单 ID">
                <el-input v-model.trim="orderForm.orderId" />
              </el-form-item>
              <el-form-item label="店铺编号">
                <el-input v-model.trim="orderForm.shopNumber" />
              </el-form-item>
            </div>
            <el-form-item label="订单金额">
              <el-input-number v-model="orderForm.orderAmount" :min="0" :precision="2" controls-position="right" />
            </el-form-item>
            <div class="form-section-title">商品明细</div>
            <div v-for="(goods, index) in orderForm.goodsList" :key="index" class="settlement-goods-row">
              <el-input v-model.trim="goods.goodsNumber" placeholder="商品编码" :data-testid="`settlement-goods-number-${index}`" />
              <el-input-number v-model="goods.goodsAmount" :min="0" :precision="2" controls-position="right" />
              <el-button :icon="Trash2" circle :disabled="orderForm.goodsList.length === 1" @click="removeGoods(index)" />
            </div>
            <el-button class="settlement-add-goods" :icon="Plus" @click="addGoods">添加商品</el-button>
          </el-form>
        </article>

        <article class="surface-card settlement-panel price-panel">
          <p class="eyebrow">Price</p>
          <h3>价格摘要</h3>
          <div class="price-line">
            <span>商品金额</span>
            <strong>¥{{ money(orderForm.orderAmount) }}</strong>
          </div>
          <button class="coupon-summary-row" type="button" @click="drawerVisible = true">
            <span>优惠券</span>
            <strong>{{ selectedCoupon ? `-¥${money(selectedDiscount)}` : '未使用' }}</strong>
          </button>
          <div class="price-line price-line--pay">
            <span>实付金额</span>
            <strong>¥{{ money(payAmount) }}</strong>
          </div>
          <el-alert
            v-if="applyResult"
            :title="`后端已应用优惠券，折后金额 ¥${money(applyResult.finalAmount)}`"
            type="success"
            show-icon
            :closable="false"
          />
          <el-button type="primary" :loading="applying" :disabled="!selectedCoupon" data-testid="apply-coupon-button" @click="applyCoupon">确认使用优惠券</el-button>
        </article>
      </div>

      <div class="surface-card table-card">
        <div class="section-heading">
          <div>
            <p class="eyebrow">Coupons</p>
            <h3>结算可用券</h3>
          </div>
          <el-button :loading="querying" :icon="Search" @click="queryCoupons">查询优惠券</el-button>
        </div>
        <el-table v-loading="querying" :data="availableCoupons" row-key="id" stripe data-testid="available-coupon-table">
          <el-table-column label="优惠" min-width="150">
            <template #default="{ row }">{{ formatSettlementCoupon(row) }}</template>
          </el-table-column>
          <el-table-column label="适用范围" min-width="160">
            <template #default="{ row }">{{ getCouponTargetText(row.target) }} {{ row.goods || '' }}</template>
          </el-table-column>
          <el-table-column label="抵扣金额" width="120">
            <template #default="{ row }">¥{{ money(row.couponAmount || 0) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="primary" @click="selectedCouponId = row.id">选择</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <el-drawer v-model="drawerVisible" title="选择优惠券" size="560px">
      <el-tabs v-model="drawerTab">
        <el-tab-pane :label="`可用券 ${availableCoupons.length}`" name="available">
          <div class="settlement-coupon-list">
            <label v-for="coupon in availableCoupons" :key="coupon.id" class="settlement-coupon-option">
              <el-radio v-model="selectedCouponId" :label="coupon.id" />
              <div>
                <strong>{{ formatSettlementCoupon(coupon) }}</strong>
                <p>{{ getCouponTargetText(coupon.target) }} {{ coupon.goods || '' }} · 可抵扣 ¥{{ money(coupon.couponAmount || 0) }}</p>
              </div>
            </label>
          </div>
        </el-tab-pane>
        <el-tab-pane :label="`不可用 ${notAvailableCoupons.length}`" name="unavailable">
          <div class="settlement-coupon-list">
            <div v-for="coupon in notAvailableCoupons" :key="coupon.id" class="settlement-coupon-option settlement-coupon-option--disabled">
              <div>
                <strong>{{ formatSettlementCoupon(coupon) }}</strong>
                <p>{{ unavailableReason(coupon) }}</p>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="selectedCouponId = ''">不使用优惠券</el-button>
        <el-button type="primary" @click="drawerVisible = false">确认</el-button>
      </template>
    </el-drawer>
  </PermissionGate>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, RefreshCw, Search, Ticket, Trash2 } from 'lucide-vue-next'
import { useRoute } from 'vue-router'
import { settlementApi } from '@/api/coupon'
import PermissionGate from '@/components/auth/PermissionGate.vue'
import { useAuthStore } from '@/stores/auth'
import type { ApplyCouponResult, SettlementCoupon, SettlementGoods } from '@/types/coupon'
import { formatCouponBenefit, getCouponTargetText, parseJsonObject } from '@/utils/coupon'

const authStore = useAuthStore()
const route = useRoute()
const CHECKOUT_STORAGE_KEY = 'youhuiquan.checkout.order'

interface CheckoutOrderDraft {
  orderId?: string
  shopNumber?: string
  orderAmount?: number
  goodsList?: SettlementGoods[]
}

const orderForm = reactive<{
  orderId: string
  shopNumber: string
  orderAmount: number
  goodsList: SettlementGoods[]
}>({
  orderId: String(Date.now()),
  shopNumber: String(route.query.shopNumber || authStore.shopNumber || '1'),
  orderAmount: 199,
  goodsList: [
    {
      goodsNumber: 'CQUPT-GOODS-001',
      goodsAmount: 199
    }
  ]
})

const querying = ref(false)
const applying = ref(false)
const drawerVisible = ref(false)
const drawerTab = ref('available')
const selectedCouponId = ref(String(route.query.couponId || ''))
const availableCoupons = ref<SettlementCoupon[]>([])
const notAvailableCoupons = ref<SettlementCoupon[]>([])
const applyResult = ref<ApplyCouponResult | null>(null)

const selectedCoupon = computed(() => availableCoupons.value.find((coupon) => String(coupon.id) === String(selectedCouponId.value)) || null)
const selectedDiscount = computed(() => Number(selectedCoupon.value?.couponAmount || 0))
const payAmount = computed(() => Math.max(Number(orderForm.orderAmount || 0) - selectedDiscount.value, 0))

function loadCheckoutOrderDraft() {
  if (route.query.from !== 'cart') {
    return
  }
  try {
    const raw = sessionStorage.getItem(CHECKOUT_STORAGE_KEY)
    if (!raw) return
    const draft = JSON.parse(raw) as CheckoutOrderDraft
    orderForm.orderId = draft.orderId || orderForm.orderId
    orderForm.shopNumber = String(draft.shopNumber || orderForm.shopNumber)
    orderForm.orderAmount = Number(draft.orderAmount || orderForm.orderAmount)
    if (draft.goodsList?.length) {
      orderForm.goodsList = draft.goodsList.map((goods) => ({
        goodsNumber: String(goods.goodsNumber || ''),
        goodsAmount: Number(goods.goodsAmount || 0)
      }))
    }
  } catch {
    ElMessage.warning('购物车结算信息读取失败，请重新选择商品')
  }
}

function addGoods() {
  orderForm.goodsList.push({
    goodsNumber: '',
    goodsAmount: 0
  })
}

function removeGoods(index: number) {
  orderForm.goodsList.splice(index, 1)
}

async function queryCoupons() {
  if (!orderForm.shopNumber) {
    ElMessage.warning('请先填写店铺编号')
    return
  }
  querying.value = true
  try {
    const result = await settlementApi.queryCoupons({
      userId: authStore.userId,
      orderAmount: Number(orderForm.orderAmount || 0),
      shopNumber: orderForm.shopNumber,
      goodsList: orderForm.goodsList.filter((goods) => goods.goodsNumber)
    })
    availableCoupons.value = result.availableCoupons || []
    notAvailableCoupons.value = result.notAvailableCoupons || []
    if (!selectedCouponId.value && availableCoupons.value.length) {
      selectedCouponId.value = String(availableCoupons.value[0].id)
    }
  } finally {
    querying.value = false
  }
}

async function applyCoupon() {
  if (!selectedCoupon.value) {
    ElMessage.warning('请先选择优惠券')
    return
  }
  applying.value = true
  try {
    applyResult.value = await settlementApi.applyCoupon(String(selectedCoupon.value.id), {
      userId: authStore.userId,
      shopNumber: orderForm.shopNumber,
      orderAmount: Number(orderForm.orderAmount || 0),
      orderId: orderForm.orderId
    })
    ElMessage.success('优惠券已应用')
  } finally {
    applying.value = false
  }
}

function formatSettlementCoupon(coupon: SettlementCoupon) {
  return coupon.consumeRule ? formatCouponBenefit(coupon.type, coupon.consumeRule) : `优惠券 ${coupon.id}`
}

function unavailableReason(coupon: SettlementCoupon) {
  const rule = parseJsonObject<Record<string, string | number>>(coupon.consumeRule)
  if (coupon.goods) return `仅指定商品 ${coupon.goods} 可用，当前订单未满足商品或金额条件`
  return rule.termsOfUse || rule.thresholdAmount ? `未达到使用门槛 ${rule.termsOfUse || rule.thresholdAmount}` : '当前订单暂不可用'
}

function money(value: string | number) {
  return Number(value || 0).toFixed(2)
}

onMounted(() => {
  loadCheckoutOrderDraft()
  void queryCoupons()
})
</script>

<style scoped>
.settlement-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.8fr);
  gap: 18px;
}

.settlement-panel {
  padding: 24px;
}

.settlement-goods-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px 40px;
  gap: 10px;
  margin-bottom: 10px;
}

.settlement-add-goods {
  width: 100%;
}

.price-panel {
  display: grid;
  align-content: start;
  gap: 16px;
  background:
    radial-gradient(circle at 92% 8%, rgba(66, 211, 146, 0.16), transparent 26%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(247, 254, 255, 0.9));
}

.price-line,
.coupon-summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 0;
  border: 0;
  border-bottom: 1px solid var(--color-border);
  color: var(--color-text);
  background: transparent;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.price-line--pay strong {
  color: var(--color-accent);
  font-family: var(--font-family-number);
  font-size: 24px;
}

.settlement-coupon-list {
  display: grid;
  gap: 10px;
}

.settlement-coupon-option {
  display: flex;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: #fff;
  transition:
    border-color var(--duration-normal) var(--ease-out),
    background var(--duration-normal) var(--ease-out),
    transform var(--duration-normal) var(--ease-out);
}

.settlement-coupon-option:hover {
  border-color: rgba(0, 167, 199, 0.35);
  background: var(--color-brand-soft);
  transform: translateY(-1px);
}

.settlement-coupon-option p {
  margin: 6px 0 0;
  color: var(--color-muted);
}

.settlement-coupon-option--disabled {
  color: var(--color-muted);
  background: #fafafa;
}

@media (max-width: 900px) {
  .settlement-grid,
  .settlement-goods-row {
    grid-template-columns: 1fr;
  }
}
</style>
