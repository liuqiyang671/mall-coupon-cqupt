<template>
  <div class="user-shell">
    <header class="user-header">
      <RouterLink class="user-brand" to="/user/products">
        <span class="user-brand__mark">邮</span>
        <span>
          <strong>邮惠精品馆</strong>
          <small>Coupon Boutique</small>
        </span>
      </RouterLink>

      <nav class="user-nav" aria-label="用户前台导航">
        <RouterLink v-for="item in navItems" :key="item.to" :to="item.to" class="user-nav__link">
          <component :is="item.icon" :size="17" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="user-header__actions">
        <el-badge :value="cartStore.totalCount" :hidden="cartStore.totalCount === 0" :max="99">
          <el-button class="user-cart-button" :icon="ShoppingCart" @click="router.push({ name: 'cart' })">购物车</el-button>
        </el-badge>
        <span class="user-account-chip">{{ displayInitial }}</span>
        <el-button :icon="LogOut" @click="handleLogout">退出</el-button>
      </div>
    </header>

    <main class="user-content">
      <RouterView />
    </main>

    <nav class="user-mobile-tabbar" aria-label="移动端用户导航">
      <RouterLink v-for="item in mobileNavItems" :key="item.to" :to="item.to" class="user-mobile-tabbar__item">
        <component :is="item.icon" :size="20" />
        <span>{{ item.label }}</span>
      </RouterLink>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { Bell, CreditCard, Gift, LogOut, Search, ShoppingCart, Ticket, UserRound } from 'lucide-vue-next'
import { ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'

const router = useRouter()
const authStore = useAuthStore()
const cartStore = useCartStore()

watch(() => authStore.isAuthenticated, (isAuthenticated) => {
  if (isAuthenticated && authStore.isCustomer && !cartStore.initialized) {
    cartStore.fetchSummary()
  }
}, { immediate: true })

const navItems = [
  { to: '/user/products', label: '商品', icon: Search },
  { to: '/user/coupon-center', label: '领券', icon: Gift },
  { to: '/user/my-coupons', label: '券包', icon: Ticket },
  { to: '/user/coupon-reminds', label: '提醒', icon: Bell },
  { to: '/user/cart', label: '购物车', icon: ShoppingCart },
  { to: '/user/settlement', label: '结算', icon: CreditCard },
  { to: '/user/profile', label: '账户', icon: UserRound }
]

const mobileNavItems = navItems.filter((item) => ['/user/products', '/user/coupon-center', '/user/cart', '/user/my-coupons', '/user/profile'].includes(item.to))

const displayInitial = computed(() => authStore.displayName.slice(0, 1).toUpperCase())

async function handleLogout() {
  await ElMessageBox.confirm('确认退出当前账号？', '退出登录', {
    confirmButtonText: '退出',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await authStore.logout()
  await router.replace({ name: 'login' })
}
</script>
