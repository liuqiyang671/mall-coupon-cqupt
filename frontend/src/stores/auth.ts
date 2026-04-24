import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'
import { USER_ROLE_OPTIONS, type AuthSession, type LoginPayload, type RegisterPayload, type UpdateProfilePayload, type UserInfo, type UserRoleType } from '@/types/auth'

const STORAGE_KEY = 'onecoupon.auth.session'

function readSession(): AuthSession | null {
  const localValue = localStorage.getItem(STORAGE_KEY)
  const sessionValue = sessionStorage.getItem(STORAGE_KEY)
  const raw = localValue || sessionValue
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as AuthSession
  } catch {
    return null
  }
}

function writeSession(session: AuthSession) {
  const target = session.remember ? localStorage : sessionStorage
  const other = session.remember ? sessionStorage : localStorage
  target.setItem(STORAGE_KEY, JSON.stringify(session))
  other.removeItem(STORAGE_KEY)
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: '',
    expireTime: '',
    userId: '',
    username: '',
    roleType: null as UserRoleType | null,
    nickname: '',
    shopNumber: null as string | null,
    status: null as number | null,
    activationStatus: null as number | null,
    userInfo: null as UserInfo | null,
    restored: false
  }),

  getters: {
    isAuthenticated: (state) => Boolean(state.token) && Number(state.expireTime || 0) > Date.now(),
    isExpired: (state) => Boolean(state.expireTime) && Number(state.expireTime) <= Date.now(),
    displayName: (state) => state.userInfo?.nickname || state.nickname || state.userInfo?.username || state.username || '邮惠券用户',
    roleLabel: (state) => USER_ROLE_OPTIONS.find((role) => role.value === state.roleType)?.label || '未识别角色',
    isMerchant: (state) => state.roleType === 1,
    isPlatform: (state) => state.roleType === 0,
    isCustomer: (state) => state.roleType === 2,
    roleWorkspaceName: (state) => {
      if (state.roleType === 0) return '平台工作台'
      if (state.roleType === 2) return '用户中心'
      return '商家后台'
    }
  },

  actions: {
    restoreSession() {
      if (this.restored) {
        return
      }
      const session = readSession()
      if (session && Number(session.expireTime) > Date.now()) {
        this.token = session.token
        this.expireTime = session.expireTime
        this.userId = session.userId
        this.username = session.username
        this.roleType = session.roleType ?? 1
        this.nickname = session.nickname || ''
        this.shopNumber = session.shopNumber
      } else {
        this.clearSession()
      }
      this.restored = true
    },

    persistSession(remember: boolean) {
      writeSession({
        token: this.token,
        expireTime: this.expireTime,
        userId: this.userId,
        username: this.username,
        roleType: this.roleType ?? 1,
        nickname: this.nickname || undefined,
        shopNumber: this.shopNumber,
        remember
      })
    },

    async login(payload: LoginPayload & { remember: boolean }) {
      const response = await authApi.login({
        roleType: payload.roleType,
        username: payload.username,
        password: payload.password
      })
      this.token = response.token
      this.expireTime = String(response.expireTime)
      this.userId = response.userId
      this.username = response.username
      this.roleType = response.roleType
      this.nickname = response.nickname || ''
      this.shopNumber = response.shopNumber ? String(response.shopNumber) : null
      this.status = response.status ?? null
      this.activationStatus = response.activationStatus ?? null
      this.persistSession(payload.remember)
      await this.fetchUserInfo()
    },

    async register(payload: RegisterPayload) {
      await authApi.register(payload)
    },

    async fetchUserInfo() {
      this.userInfo = await authApi.getUserInfo()
      this.username = this.userInfo.username
      this.roleType = this.userInfo.roleType
      this.nickname = this.userInfo.nickname || ''
      this.shopNumber = this.userInfo.shopNumber ? String(this.userInfo.shopNumber) : null
      this.status = this.userInfo.status ?? null
      this.activationStatus = this.userInfo.activationStatus ?? null
    },

    async updateProfile(payload: UpdateProfilePayload) {
      await authApi.updateProfile(payload)
      await this.fetchUserInfo()
    },

    async changePassword(oldPassword: string, newPassword: string) {
      await authApi.changePassword({ oldPassword, newPassword })
      this.clearSession()
    },

    async logout() {
      try {
        if (this.token) {
          await authApi.logout()
        }
      } finally {
        this.clearSession()
      }
    },

    clearSession() {
      this.token = ''
      this.expireTime = ''
      this.userId = ''
      this.username = ''
      this.roleType = null
      this.nickname = ''
      this.shopNumber = null
      this.status = null
      this.activationStatus = null
      this.userInfo = null
      localStorage.removeItem(STORAGE_KEY)
      sessionStorage.removeItem(STORAGE_KEY)
    }
  }
})
