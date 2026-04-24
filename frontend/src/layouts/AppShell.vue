<template>
  <div class="app-shell">
    <aside class="app-sidebar">
      <RouterLink class="app-logo" to="/merchant">
        <span class="brand-mark brand-mark--small">邮</span>
        <span>邮惠券商城</span>
      </RouterLink>
      <nav class="app-nav">
        <RouterLink v-for="item in navItems" :key="item.to" :to="item.to">{{ item.label }}</RouterLink>
      </nav>
    </aside>

    <div class="app-main">
      <header class="app-header">
        <div>
          <p class="eyebrow">{{ authStore.roleWorkspaceName }}</p>
          <h2>{{ route.meta.title || authStore.roleWorkspaceName }}</h2>
        </div>
        <div class="app-header__actions">
          <span class="role-badge">{{ authStore.roleLabel }}</span>
          <span class="user-pill">{{ authStore.displayName }}</span>
          <el-button :icon="LogOut" @click="handleLogout">退出登录</el-button>
        </div>
      </header>
      <RouterView />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { LogOut } from 'lucide-vue-next'
import { ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const navItems = computed(() => [
  { to: '/merchant', label: authStore.roleWorkspaceName },
  { to: '/merchant/profile', label: '个人信息' }
])

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
