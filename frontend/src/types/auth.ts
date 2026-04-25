export interface ApiResult<T> {
  code: string
  message: string | null
  data: T
  requestId?: string
}

export type UserRoleType = 0 | 1 | 2

export const USER_ROLE_OPTIONS: Array<{
  value: UserRoleType
  label: string
  loginHint: string
  registerHint: string
}> = [
  {
    value: 1,
    label: '商家',
    loginHint: '进入优惠券模板和推送任务管理后台。',
    registerHint: '注册后即可维护店铺优惠券活动。'
  },
  {
    value: 0,
    label: '平台人员',
    loginHint: '进入平台运营与账号管理工作台。',
    registerHint: '用于平台运营、审核和管理场景。'
  },
  {
    value: 2,
    label: '用户',
    loginHint: '进入商品商城、购物车和个人券包。',
    registerHint: '用于浏览商品、领券、用券和维护个人资料。'
  }
]

export interface LoginPayload {
  roleType: UserRoleType
  username: string
  password: string
}

export interface RegisterPayload {
  roleType: UserRoleType
  username: string
  password: string
  nickname?: string
  realName?: string
  shopNumber?: string
  phone?: string
  mail?: string
}

export interface LoginResponse {
  token: string
  expireTime: string
  userId: string
  username: string
  roleType: UserRoleType
  nickname?: string
  shopNumber: string | null
  status?: number
  activationStatus?: number
}

export interface UserInfo {
  userId: string
  username: string
  roleType: UserRoleType
  nickname?: string
  realName?: string
  shopNumber: string | null
  phone?: string
  mail?: string
  avatarUrl?: string
  status?: number
  activationStatus?: number
  lastLoginTime?: string
  createTime?: string
  updateTime?: string
}

export interface UpdateProfilePayload {
  phone?: string
  mail?: string
  nickname?: string
  realName?: string
  avatarUrl?: string
}

export interface ChangePasswordPayload {
  oldPassword: string
  newPassword: string
}

export interface AuthSession {
  token: string
  expireTime: string
  userId: string
  username: string
  roleType: UserRoleType
  nickname?: string
  shopNumber: string | null
  remember: boolean
}
