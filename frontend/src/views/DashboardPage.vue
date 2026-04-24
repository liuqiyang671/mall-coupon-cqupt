<template>
  <section class="dashboard-grid">
    <article class="surface-card dashboard-card dashboard-card--wide">
      <p class="eyebrow">欢迎回来</p>
      <h3>{{ authStore.displayName }}</h3>
      <p class="muted">{{ workspaceDescription }}</p>
    </article>
    <article class="surface-card dashboard-card">
      <p class="metric-label">用户 ID</p>
      <strong>{{ authStore.userId || '-' }}</strong>
    </article>
    <article class="surface-card dashboard-card">
      <p class="metric-label">账号身份</p>
      <strong>{{ authStore.roleLabel }}</strong>
    </article>
    <article v-if="authStore.isMerchant" class="surface-card dashboard-card">
      <p class="metric-label">店铺编号</p>
      <strong>{{ authStore.shopNumber || '系统自动生成后可用' }}</strong>
    </article>
    <article class="surface-card dashboard-card">
      <p class="metric-label">账号状态</p>
      <strong>{{ accountStatusText }}</strong>
    </article>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const workspaceDescription = computed(() => {
  if (authStore.isPlatform) {
    return '这里是邮惠券商城平台工作台，当前账号可用于平台运营、账号审核和全局管理能力接入。'
  }
  if (authStore.isCustomer) {
    return '这里是邮惠券商城用户中心，当前账号可用于个人资料维护、券包和领券页面接入。'
  }
  return '这里是邮惠券商城商家后台，当前账号可管理优惠券模板和推送任务。'
})

const accountStatusText = computed(() => {
  if (authStore.status === 1) return '已禁用'
  if (authStore.activationStatus === 0) return '未激活'
  return '正常'
})

onMounted(() => {
  if (!authStore.userInfo) {
    void authStore.fetchUserInfo()
  }
})
</script>
