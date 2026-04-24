import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { UserRoleType } from '@/types/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/merchant'
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
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'merchant-home',
        component: () => import('@/views/DashboardPage.vue'),
        meta: { title: '工作台' }
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
    path: '/:pathMatch(.*)*',
    redirect: '/merchant'
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
    return { name: 'merchant-home' }
  }

  if (to.meta.guestOnly && authStore.isAuthenticated) {
    return { name: 'merchant-home' }
  }

  return true
})

export default router
