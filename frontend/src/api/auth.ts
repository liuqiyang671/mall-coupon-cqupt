import { http } from './http'
import type {
  ChangePasswordPayload,
  LoginPayload,
  LoginResponse,
  RegisterPayload,
  UpdateProfilePayload,
  UserInfo
} from '@/types/auth'

export const authApi = {
  login(payload: LoginPayload) {
    return http.post<unknown, LoginResponse>('/api/merchant-admin/user/login', payload)
  },

  register(payload: RegisterPayload) {
    return http.post<unknown, void>('/api/merchant-admin/user/register', payload)
  },

  getUserInfo() {
    return http.get<unknown, UserInfo>('/api/merchant-admin/user/info')
  },

  updateProfile(payload: UpdateProfilePayload) {
    return http.post<unknown, void>('/api/merchant-admin/user/update', payload)
  },

  changePassword(payload: ChangePasswordPayload) {
    return http.post<unknown, void>('/api/merchant-admin/user/change-password', payload)
  },

  logout() {
    return http.post<unknown, void>('/api/merchant-admin/user/logout')
  }
}
