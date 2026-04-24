export const usernamePattern = /^[a-zA-Z0-9_]+$/
export const phonePattern = /^1[3-9]\d{9}$/
export const mailPattern = /^[\w.-]+@[\w.-]+\.\w+$/
export const shopNumberPattern = /^\d+$/

export function validateUsername(value: string) {
  if (!value) return '请输入用户名'
  if (value.length < 3 || value.length > 32) return '用户名长度必须在 3-32 位之间'
  if (!usernamePattern.test(value)) return '用户名只能包含字母、数字和下划线'
  return ''
}

export function validatePassword(value: string, label = '密码') {
  if (!value) return `请输入${label}`
  if (value.length < 6 || value.length > 32) return `${label}长度必须在 6-32 位之间`
  return ''
}

export function validatePhone(value?: string) {
  if (!value) return ''
  return phonePattern.test(value) ? '' : '手机号格式不正确'
}

export function validateMail(value?: string) {
  if (!value) return ''
  return mailPattern.test(value) ? '' : '邮箱格式不正确'
}

export function validateShopNumber(value?: string) {
  if (!value) return ''
  return shopNumberPattern.test(value) ? '' : '店铺编号必须为数字'
}
