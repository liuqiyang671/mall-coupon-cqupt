<template>
  <PermissionGate :roles="[2]">
    <section class="coupon-page cart-page">
      <div class="surface-card coupon-toolbar">
        <div>
          <p class="eyebrow">Cart</p>
          <h3>购物车</h3>
          <p class="muted">管理待结算商品，调整数量后可进入优惠券结算。</p>
        </div>
        <div class="toolbar-actions">
          <el-tag type="info">共 {{ cartStore.totalCount }} 件商品</el-tag>
          <el-button :icon="RefreshCw" @click="cartStore.fetchSummary">刷新</el-button>
        </div>
      </div>

      <div class="cart-grid">
        <article class="surface-card cart-main">
          <div class="section-heading">
            <div>
              <p class="eyebrow">Items</p>
              <h3>商品清单</h3>
            </div>
            <div v-if="!cartStore.isEmpty" class="cart-actions">
              <el-button
                v-if="cartStore.selectedCount > 0"
                link
                type="danger"
                @click="cartStore.removeSelected()"
              >
                删除选中
              </el-button>
              <el-button link type="danger" @click="cartStore.clearCart()">清空购物车</el-button>
            </div>
          </div>

          <div v-loading="cartStore.loading" class="cart-content">
            <el-empty v-if="cartStore.isEmpty && cartStore.initialized" description="购物车是空的" />

            <template v-else>
              <div class="cart-list-head">
                <el-checkbox
                  :model-value="cartStore.isAllSelected"
                  :indeterminate="isIndeterminate"
                  @change="cartStore.toggleSelectAll()"
                >
                  全选
                </el-checkbox>
                <span>单价</span>
                <span>数量</span>
                <span>小计</span>
              </div>

              <div class="cart-list">
                <article
                  v-for="item in cartStore.items"
                  :key="item.id"
                  class="cart-item"
                  :class="{
                    'cart-item--selected': item.selected === 1 && item.goodsStatus === 1,
                    'cart-item--disabled': item.goodsStatus !== 1
                  }"
                >
                  <div class="cart-item__product">
                    <el-checkbox
                      :model-value="item.selected === 1"
                      :disabled="item.goodsStatus !== 1"
                      @change="cartStore.toggleSelect(item.id)"
                    />
                    <div class="cart-item__image">
                      <img v-if="item.mainImage" :src="item.mainImage" :alt="item.goodsName" @error="onImageError" />
                      <ShoppingBag v-else :size="24" />
                    </div>
                    <div class="cart-item__info">
                      <strong>{{ item.goodsName }}</strong>
                      <span>商品 ID：{{ item.goodsId }} · 店铺 {{ item.shopNumber }}</span>
                      <div class="cart-item__tags">
                        <el-tag :type="goodsStatusTagType(item.goodsStatus)" size="small">{{ goodsStatusText(item.goodsStatus) }}</el-tag>
                        <el-tag v-if="item.goodsStatus === 1 && item.goodsStock <= 10" type="warning" size="small">
                          仅剩 {{ item.goodsStock }} 件
                        </el-tag>
                      </div>
                    </div>
                  </div>

                  <div class="cart-item__price">
                    <strong>¥{{ money(item.price) }}</strong>
                    <span v-if="item.originalPrice && item.originalPrice > item.price">¥{{ money(item.originalPrice) }}</span>
                  </div>

                  <div class="cart-item__quantity">
                    <el-input-number
                      v-if="item.goodsStatus === 1"
                      :model-value="item.quantity"
                      :min="1"
                      :max="Math.min(item.goodsStock || 999, 999)"
                      :step="1"
                      size="small"
                      controls-position="right"
                      @change="(value: number | undefined) => handleQuantityChange(item.id, value)"
                    />
                    <span v-else>x{{ item.quantity }}</span>
                  </div>

                  <div class="cart-item__subtotal">
                    <strong v-if="item.goodsStatus === 1">¥{{ money(item.subtotal) }}</strong>
                    <span v-else>-</span>
                    <el-button :icon="Trash2" circle text type="danger" @click="cartStore.removeItem(item.id)" />
                  </div>
                </article>
              </div>
            </template>
          </div>
        </article>

        <aside class="cart-side">
          <article class="surface-card cart-panel">
            <p class="eyebrow">Browse</p>
            <h3>继续挑选</h3>
            <p class="muted">从商品商城加购后，购物车会自动同步商品信息、价格和库存状态。</p>
            <el-button type="primary" :icon="ShoppingBag" @click="router.push({ name: 'user-products' })">返回商品商城</el-button>
          </article>

          <article class="surface-card cart-panel">
            <p class="eyebrow">Summary</p>
            <h3>结算摘要</h3>
            <div class="summary-lines">
              <div>
                <span>已选商品</span>
                <strong>{{ cartStore.selectedCount }} 件</strong>
              </div>
              <div>
                <span>商品金额</span>
                <strong>¥{{ money(cartStore.selectedAmount) }}</strong>
              </div>
              <div v-if="cartStore.savedAmount > 0">
                <span>已优惠</span>
                <strong class="success-text">¥{{ money(cartStore.savedAmount) }}</strong>
              </div>
              <div>
                <span>税费</span>
                <strong>¥{{ money(cartStore.taxAmount) }}</strong>
              </div>
              <div class="summary-lines__pay">
                <span>应付预估</span>
                <strong>¥{{ money(cartStore.payableAmount) }}</strong>
              </div>
            </div>
            <el-button
              type="primary"
              size="large"
              :disabled="cartStore.selectedCount === 0"
              @click="handleCheckout"
            >
              去结算
            </el-button>
          </article>
        </aside>
      </div>
    </section>
  </PermissionGate>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { RefreshCw, ShoppingBag, Trash2 } from 'lucide-vue-next'
import PermissionGate from '@/components/auth/PermissionGate.vue'
import { useCartStore } from '@/stores/cart'
import type { CartItem } from '@/types/cart'

const CHECKOUT_STORAGE_KEY = 'youhuiquan.checkout.order'

const router = useRouter()
const cartStore = useCartStore()

const isIndeterminate = computed(() => {
  const validItems = cartStore.items.filter((item) => item.goodsStatus === 1)
  const selectedValid = validItems.filter((item) => item.selected === 1)
  return selectedValid.length > 0 && selectedValid.length < validItems.length
})

let quantityTimer: ReturnType<typeof setTimeout> | null = null
const pendingQuantities = new Map<string, number>()

onMounted(() => {
  void cartStore.fetchSummary()
})

function handleQuantityChange(cartId: string, value: number | undefined) {
  if (!value || value < 1) return
  pendingQuantities.set(cartId, value)
  if (quantityTimer) clearTimeout(quantityTimer)
  quantityTimer = setTimeout(() => {
    for (const [id, quantity] of pendingQuantities) {
      void cartStore.updateQuantity(id, quantity)
    }
    pendingQuantities.clear()
  }, 350)
}

function handleCheckout() {
  const selectedItems = cartStore.items.filter((item) => item.selected === 1 && item.goodsStatus === 1)
  if (!selectedItems.length) {
    ElMessage.warning('请先选择商品')
    return
  }
  const shopNumbers = new Set(selectedItems.map((item) => String(item.shopNumber)))
  if (shopNumbers.size > 1) {
    ElMessage.warning('请按店铺分开结算')
    return
  }

  sessionStorage.setItem(CHECKOUT_STORAGE_KEY, JSON.stringify(buildCheckoutOrder(selectedItems)))
  void router.push({
    name: 'settlement',
    query: {
      from: 'cart',
      shopNumber: String(selectedItems[0].shopNumber)
    }
  })
}

function buildCheckoutOrder(items: CartItem[]) {
  return {
    orderId: String(Date.now()),
    shopNumber: String(items[0].shopNumber),
    orderAmount: Number(cartStore.selectedAmount || 0),
    goodsList: items.map((item) => ({
      goodsNumber: String(item.goodsId),
      goodsAmount: Number(item.subtotal || Number(item.price || 0) * Number(item.quantity || 0))
    })),
    cartIds: items.map((item) => item.id)
  }
}

function money(value?: number) {
  return Number(value || 0).toFixed(2)
}

function goodsStatusText(status?: number) {
  if (status === 1) return '可购买'
  if (status === 2) return '违规下架'
  return '已下架'
}

function goodsStatusTagType(status?: number) {
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  return 'info'
}

function onImageError(event: Event) {
  const image = event.target as HTMLImageElement
  image.style.display = 'none'
}
</script>

<style scoped>
.cart-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 18px;
}

.cart-main,
.cart-panel {
  padding: 24px;
}

.cart-content {
  min-height: 260px;
}

.cart-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cart-list-head {
  display: grid;
  grid-template-columns: minmax(300px, 1fr) 120px 150px 130px;
  gap: 12px;
  align-items: center;
  padding: 0 14px 12px;
  color: var(--color-muted);
  font-size: 13px;
}

.cart-list {
  display: grid;
  gap: 12px;
}

.cart-item {
  display: grid;
  grid-template-columns: minmax(300px, 1fr) 120px 150px 130px;
  gap: 12px;
  align-items: center;
  padding: 14px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(247, 254, 255, 0.86));
  transition:
    border-color var(--duration-normal) var(--ease-out),
    background var(--duration-normal) var(--ease-out),
    box-shadow var(--duration-normal) var(--ease-out),
    transform var(--duration-normal) var(--ease-out);
}

.cart-item:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.cart-item--selected {
  border-color: rgba(0, 167, 199, 0.44);
  background:
    linear-gradient(135deg, rgba(231, 251, 255, 0.94), rgba(235, 255, 246, 0.82));
}

.cart-item--disabled {
  opacity: 0.62;
}

.cart-item__product {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.cart-item__image {
  display: grid;
  flex: 0 0 auto;
  width: 70px;
  height: 70px;
  place-items: center;
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  color: var(--color-muted);
  background: #fafafa;
}

.cart-item__image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cart-item__info {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.cart-item__info strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cart-item__info span,
.cart-item__price span,
.cart-item__quantity span,
.cart-item__subtotal span {
  color: var(--color-muted);
  font-size: 12px;
}

.cart-item__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.cart-item__price {
  display: grid;
  gap: 4px;
}

.cart-item__price strong,
.cart-item__subtotal strong,
.summary-lines__pay strong {
  color: var(--color-brand);
}

.cart-item__price span {
  text-decoration: line-through;
}

.cart-item__subtotal {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.cart-side {
  display: grid;
  align-content: start;
  gap: 18px;
}

.cart-panel {
  display: grid;
  gap: 16px;
}

.cart-panel h3 {
  margin: 0;
}

.cart-panel :deep(.el-input-number) {
  width: 100%;
}

.summary-lines {
  display: grid;
  gap: 12px;
}

.summary-lines div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border);
}

.summary-lines span {
  color: var(--color-muted);
}

.summary-lines__pay strong {
  font-size: 24px;
}

.success-text {
  color: var(--color-success);
}

@media (max-width: 1100px) {
  .cart-grid,
  .cart-list-head,
  .cart-item {
    grid-template-columns: 1fr;
  }

  .cart-list-head {
    display: none;
  }
}
</style>
