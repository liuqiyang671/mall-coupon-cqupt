import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { UserRoleType } from '@/types/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/user/products'
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginPage.vue'),
    meta: { guestOnly: true, title: '登录' }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/auth/RegisterPage.vue'),
    meta: { guestOnly: true, title: '注册' }
  },
  {
    path: '/forgot-password',
    name: 'forgot-password',
    component: () => import('@/views/auth/ForgotPasswordPage.vue'),
    meta: { guestOnly: true, title: '找回密码' }
  },
  {
    path: '/merchant',
    component: () => import('@/layouts/AppShell.vue'),
    meta: { requiresAuth: true, roles: [0, 1] },
    children: [
      {
        path: '',
        name: 'merchant-home',
        component: () => import('@/views/DashboardPage.vue'),
        meta: { title: '工作台' }
      },
      {
        path: 'coupon-templates',
        name: 'coupon-templates',
        component: () => import('@/views/merchant/CouponTemplatePage.vue'),
        meta: { title: '优惠券模板', roles: [0, 1] }
      },
      {
        path: 'coupon-tasks',
        name: 'coupon-tasks',
        component: () => import('@/views/merchant/CouponTaskPage.vue'),
        meta: { title: '推送任务管理', roles: [1] }
      },
      {
        path: 'goods',
        name: 'goods-management',
        component: () => import('@/views/merchant/GoodsManagementPage.vue'),
        meta: { title: '商品管理', roles: [1] }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/auth/ProfilePage.vue'),
        meta: { title: '个人信息' }
      }
    ]
  },
  {
    path: '/user',
    component: () => import('@/layouts/UserShell.vue'),
    meta: { requiresAuth: true, roles: [2] },
    children: [
      {
        path: '',
        redirect: { name: 'user-products' }
      },
      {
        path: 'products',
        name: 'user-products',
        component: () => import('@/views/user/ProductBrowsePage.vue'),
        meta: { title: '商品商城' }
      },
      {
        path: 'coupon-center',
        name: 'coupon-center',
        component: () => import('@/views/user/CouponCenterPage.vue'),
        meta: { title: '领券中心' }
      },
      {
        path: 'my-coupons',
        name: 'my-coupons',
        component: () => import('@/views/user/MyCouponsPage.vue'),
        meta: { title: '我的优惠券' }
      },
      {
        path: 'coupon-reminds',
        name: 'coupon-reminds',
        component: () => import('@/views/user/CouponRemindPage.vue'),
        meta: { title: '预约提醒' }
      },
      {
        path: 'cart',
        name: 'cart',
        component: () => import('@/views/user/CartPage.vue'),
        meta: { title: '购物车' }
      },
      {
        path: 'settlement',
        name: 'settlement',
        component: () => import('@/views/user/SettlementPage.vue'),
        meta: { title: '优惠券结算' }
      },
      {
        path: 'profile',
        name: 'user-profile',
        component: () => import('@/views/auth/ProfilePage.vue'),
        meta: { title: '账户信息' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/user/products'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  authStore.restoreSession()

  document.title = `${to.meta.title || authStore.roleWorkspaceName || '工作台'} - 邮惠券商城`

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return {
      name: 'login',
      query: { redirect: to.fullPath }
    }
  }

  const requiredRoles = to.meta.roles as UserRoleType[] | undefined
  if (requiredRoles?.length && (authStore.roleType === null || !requiredRoles.includes(authStore.roleType))) {
    return { name: defaultHomeRouteName(authStore.roleType) }
  }

  if (to.meta.guestOnly && authStore.isAuthenticated) {
    return { name: defaultHomeRouteName(authStore.roleType) }
  }

  return true
})

function defaultHomeRouteName(roleType: UserRoleType | null) {
  return roleType === 2 ? 'user-products' : 'merchant-home'
}

export default router
