<template>
  <section class="dashboard-grid">
    <article class="surface-card dashboard-card dashboard-card--wide dashboard-hero">
      <div>
        <p class="eyebrow">Mission Control</p>
        <h3>欢迎回来，{{ authStore.displayName }}</h3>
        <p class="muted">{{ workspaceDescription }}</p>
      </div>
      <div class="dashboard-hero__ticket" aria-label="当前空间">
        <strong>{{ authStore.roleLabel }}</strong>
        <span>{{ authStore.roleWorkspaceName }}</span>
      </div>
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
    <article class="surface-card dashboard-card dashboard-card--wide">
      <div class="section-heading">
        <div>
          <p class="eyebrow">Command Modules</p>
          <h3>功能模块</h3>
        </div>
      </div>
      <div class="module-link-grid">
        <RouterLink v-for="module in modules" :key="module.to" :to="module.to" class="module-link-card">
          <component :is="module.icon" :size="22" />
          <strong>{{ module.title }}</strong>
          <span>{{ module.description }}</span>
        </RouterLink>
      </div>
    </article>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { Bell, Gift, Package, Send, ShoppingCart, Ticket, UserRound } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const workspaceDescription = computed(() => {
  if (authStore.isPlatform) {
    return '这里是邮惠券商城平台工作台，当前账号可用于平台运营、账号审核和全局管理能力接入。'
  }
  if (authStore.isCustomer) {
    return '这里是邮惠券商城用户中心，当前账号可用于个人资料维护、券包和领券页面接入。'
  }
  return '这里是邮惠券商城商家后台，当前账号可管理商品、优惠券模板和推送任务。'
})

const accountStatusText = computed(() => {
  if (authStore.status === 1) return '已禁用'
  if (authStore.activationStatus === 0) return '未激活'
  return '正常'
})

const modules = computed(() => {
  if (authStore.isCustomer) {
    return [
      { to: '/user/products', title: '商品商城', description: '浏览商品并加入购物车', icon: ShoppingCart },
      { to: '/user/coupon-center', title: '领券中心', description: '浏览并领取可用优惠券', icon: Gift },
      { to: '/user/my-coupons', title: '我的优惠券', description: '查看已领取券和券状态', icon: Ticket },
      { to: '/user/coupon-reminds', title: '预约提醒', description: '管理开抢提醒', icon: Bell },
      { to: '/user/cart', title: '购物车', description: '管理待结算商品', icon: ShoppingCart },
      { to: '/user/profile', title: '个人信息', description: '维护账号资料和密码', icon: UserRound }
    ]
  }
  if (authStore.isMerchant) {
    return [
      { to: '/merchant/coupon-templates', title: '优惠券模板', description: '维护当前店铺券模板', icon: Ticket },
      { to: '/merchant/goods', title: '商品管理', description: '维护商品、分类和库存', icon: Package },
      { to: '/merchant/coupon-tasks', title: '推送任务管理', description: '批量发券与任务跟踪', icon: Send },
      { to: '/merchant/profile', title: '个人信息', description: '维护商家账号资料', icon: UserRound }
    ]
  }
  return [
    { to: '/merchant/coupon-templates', title: '平台券模板', description: '维护平台级优惠券规则', icon: Ticket },
    { to: '/merchant/profile', title: '个人信息', description: '维护平台账号资料', icon: UserRound }
  ]
})

onMounted(() => {
  if (!authStore.userInfo) {
    void authStore.fetchUserInfo()
  }
})
</script>
