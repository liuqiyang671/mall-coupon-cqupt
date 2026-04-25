<template>
  <div class="app-shell">
    <aside class="app-sidebar">
      <RouterLink class="app-logo" to="/merchant">
        <span class="brand-mark brand-mark--small">邮</span>
        <span class="app-logo__text">
          <strong>邮惠券商城</strong>
          <span>Coupon Matrix</span>
        </span>
      </RouterLink>
      <nav class="app-nav">
        <RouterLink v-for="item in navItems" :key="item.to" :to="item.to" class="app-nav__link">
          <component :is="item.icon" :size="18" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>
      <div class="app-sidebar__footer">
        <div class="app-session-card">
          <span class="app-session-card__avatar">{{ displayInitial }}</span>
          <div>
            <strong>{{ authStore.displayName }}</strong>
            <span>{{ authStore.roleLabel }}</span>
          </div>
        </div>
      </div>
    </aside>

    <div class="app-main">
      <header class="app-header">
        <div class="app-header__title">
          <p class="eyebrow">{{ authStore.roleWorkspaceName }}</p>
          <h2>{{ route.meta.title || authStore.roleWorkspaceName }}</h2>
        </div>
        <div class="app-header__actions">
          <span class="role-badge">{{ authStore.roleLabel }}</span>
          <span class="user-pill">{{ authStore.displayName }}</span>
          <el-button :icon="LogOut" @click="handleLogout">退出登录</el-button>
        </div>
      </header>
      <main class="app-content">
        <RouterView />
      </main>
      <nav class="mobile-tabbar" aria-label="移动端导航">
        <RouterLink v-for="item in mobileNavItems" :key="item.to" :to="item.to" class="mobile-tabbar__item">
          <component :is="item.icon" :size="20" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  LayoutDashboard,
  LogOut,
  Package,
  Send,
  Ticket,
  UserRound
} from 'lucide-vue-next'
import { ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const navItems = computed(() => [
  { to: '/merchant', label: authStore.roleWorkspaceName, icon: LayoutDashboard },
  ...(authStore.isMerchant || authStore.isPlatform ? [{ to: '/merchant/coupon-templates', label: '优惠券模板', icon: Ticket }] : []),
  ...(authStore.isMerchant ? [{ to: '/merchant/goods', label: '商品管理', icon: Package }] : []),
  ...(authStore.isMerchant ? [{ to: '/merchant/coupon-tasks', label: '推送任务管理', icon: Send }] : []),
  { to: '/merchant/profile', label: '个人信息', icon: UserRound }
])

const mobileNavItems = computed(() => {
  return navItems.value.slice(0, 5)
})

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
