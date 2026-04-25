import type {
  ConsumeRuleDraft,
  CouponStatus,
  CouponTarget,
  CouponTaskNotifyType,
  CouponTaskSendType,
  CouponTaskStatus,
  CouponType,
  ReceiveRuleDraft,
  UserCouponStatus
} from '@/types/coupon'

export const couponSourceText: Record<number, string> = {
  0: '店铺券',
  1: '平台券'
}

export const couponTargetText: Record<CouponTarget, string> = {
  0: '商品专属',
  1: '全店通用'
}

export const couponTypeText: Record<CouponType, string> = {
  0: '立减券',
  1: '满减券',
  2: '折扣券'
}

export const couponStatusText: Record<CouponStatus, string> = {
  0: '生效中',
  1: '已结束'
}

export const distributionModeText: Record<string, string> = {
  RECEIVE: '用户领取',
  AUTO: '自动发放',
  ACTIVITY: '活动发放'
}

export const applicableMerchantScopeText: Record<string, string> = {
  ALL: '全部商家',
  SPECIFIED: '指定商家'
}

export const couponTaskStatusText: Record<CouponTaskStatus, string> = {
  0: '待执行',
  1: '执行中',
  2: '执行失败',
  3: '执行成功',
  4: '已取消'
}

export const couponTaskSendTypeText: Record<CouponTaskSendType, string> = {
  0: '立即发送',
  1: '定时发送'
}

export const couponTaskNotifyTypeText: Record<CouponTaskNotifyType, string> = {
  0: '站内信',
  1: '弹框推送',
  2: '邮箱',
  3: '短信'
}

export const userCouponStatusText: Record<UserCouponStatus, string> = {
  0: '未使用',
  1: '已锁定',
  2: '已使用',
  3: '已过期',
  4: '已撤回'
}

export function getCouponSourceText(source?: number) {
  return couponSourceText[source ?? 0] || '未知来源'
}

export function getCouponTargetText(target?: number) {
  return couponTargetText[(target ?? 1) as CouponTarget] || '未知范围'
}

export function getCouponTypeText(type?: number) {
  return couponTypeText[(type ?? 1) as CouponType] || '未知类型'
}

export function getCouponStatusText(status?: number) {
  return couponStatusText[(status ?? 1) as CouponStatus] || '未知状态'
}

export function couponStatusTagType(status?: number) {
  if (status === 0) return 'success'
  if (status === 1) return 'info'
  if (status === 2) return 'warning'
  return 'info'
}

export function getCouponTaskStatusText(status?: number) {
  return couponTaskStatusText[(status ?? 0) as CouponTaskStatus] || '未知状态'
}

export function couponTaskStatusTagType(status?: number) {
  if (status === 0) return 'warning'
  if (status === 1) return 'primary'
  if (status === 2) return 'danger'
  if (status === 3) return 'success'
  if (status === 4) return 'info'
  return 'info'
}

export function getCouponTaskSendTypeText(sendType?: number) {
  return couponTaskSendTypeText[(sendType ?? 0) as CouponTaskSendType] || '未知发送方式'
}

export function formatCouponTaskNotifyTypes(notifyType?: string) {
  if (!notifyType) return '-'
  return notifyType
    .split(',')
    .map((item) => couponTaskNotifyTypeText[item.trim() as CouponTaskNotifyType])
    .filter(Boolean)
    .join('、') || '-'
}

export function getUserCouponStatusText(status?: number) {
  return userCouponStatusText[(status ?? 0) as UserCouponStatus] || '未知状态'
}

export function userCouponStatusTagType(status?: number) {
  if (status === 0) return 'success'
  if (status === 1) return 'warning'
  if (status === 2) return 'info'
  if (status === 3) return 'danger'
  if (status === 4) return 'info'
  return 'info'
}

export function parseJsonObject<T extends Record<string, unknown>>(raw?: string): Partial<T> {
  if (!raw) return {}
  try {
    const value = JSON.parse(raw)
    return value && typeof value === 'object' && !Array.isArray(value) ? value : {}
  } catch {
    return {}
  }
}

export function buildReceiveRule(rule: ReceiveRuleDraft) {
  return JSON.stringify({
    limitPerPerson: Number(rule.limitPerPerson || 1),
    usageInstructions: rule.usageInstructions || '按页面展示规则使用',
    distributionMode: rule.distributionMode,
    applicableMerchantScope: rule.applicableMerchantScope,
    applicableShopNumbers: rule.applicableShopNumbers
  })
}

export function buildConsumeRule(type: CouponType, rule: ConsumeRuleDraft) {
  const thresholdAmount = type === 0 ? 0 : Number(rule.termsOfUse || 0)
  const discountAmount = Number(rule.maximumDiscountAmount || 0)
  const maxDiscountAmount = Number(rule.maximumDiscountAmount || 0)
  return JSON.stringify({
    termsOfUse: thresholdAmount,
    thresholdAmount,
    maximumDiscountAmount: maxDiscountAmount,
    maxDiscountAmount,
    discountAmount,
    discountRate: type === 2 ? Number(rule.discountRate || 0) : undefined,
    explanationOfUnmetConditions: rule.explanationOfUnmetConditions || '未满足优惠券使用条件'
  })
}

export function formatCouponBenefit(type: CouponType, consumeRule?: string) {
  const rule = parseJsonObject<Record<string, string | number>>(consumeRule)
  if (type === 2) {
    return `${rule.discountRate || '-'}折`
  }
  const amount = rule.maximumDiscountAmount || rule.discountAmount || '-'
  if (type === 1) {
    return `满 ${rule.termsOfUse || rule.thresholdAmount || '-'} 减 ${amount}`
  }
  return `立减 ${amount}`
}

export function formatReceiveLimit(receiveRule?: string) {
  const rule = parseJsonObject<Record<string, string | number>>(receiveRule)
  return rule.limitPerPerson ? `每人限领 ${rule.limitPerPerson} 张` : '不限领取次数'
}

export function formatUsageInstructions(receiveRule?: string) {
  const rule = parseJsonObject<Record<string, string | number>>(receiveRule)
  return String(rule.usageInstructions || '按页面展示规则使用')
}

export function formatDistributionMode(receiveRule?: string) {
  const rule = parseJsonObject<Record<string, string | number>>(receiveRule)
  return distributionModeText[String(rule.distributionMode || 'RECEIVE')] || '用户领取'
}

export function formatApplicableMerchant(receiveRule?: string) {
  const rule = parseJsonObject<Record<string, string | number>>(receiveRule)
  const scope = String(rule.applicableMerchantScope || 'ALL')
  if (scope === 'SPECIFIED') {
    return rule.applicableShopNumbers ? `指定商家：${rule.applicableShopNumbers}` : '指定商家'
  }
  return applicableMerchantScopeText[scope] || '全部商家'
}
