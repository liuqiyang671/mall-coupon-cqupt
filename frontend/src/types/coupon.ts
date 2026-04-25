export type CouponSource = 0 | 1
export type CouponTarget = 0 | 1
export type CouponType = 0 | 1 | 2
export type CouponStatus = 0 | 1
export type CouponTaskStatus = 0 | 1 | 2 | 3 | 4
export type CouponTaskSendType = 0 | 1
export type CouponTaskNotifyType = '0' | '1' | '2' | '3'
export type UserCouponStatus = 0 | 1 | 2 | 3 | 4
export type ApplicableMerchantScope = 'ALL' | 'SPECIFIED'
export type DistributionMode = 'RECEIVE' | 'AUTO' | 'ACTIVITY'

export interface PageResult<T> {
  records: T[]
  total: string | number
  size: string | number
  current: string | number
  pages?: string | number
}

export interface CouponTemplateQuery {
  current: number
  size: number
  source?: CouponSource
  name?: string
  target?: CouponTarget
  goods?: string
  type?: CouponType
}

export interface CouponCenterQuery {
  current: number
  size: number
  shopNumber?: string
  couponTemplateId?: string
  name?: string
  source?: CouponSource
  target?: CouponTarget
  type?: CouponType
}

export interface CouponTemplateSavePayload {
  name: string
  source: CouponSource
  target: CouponTarget
  goods?: string
  type: CouponType
  validStartTime: string
  validEndTime: string
  stock: number
  receiveRule: string
  consumeRule: string
}

export interface CouponTemplate {
  id?: string
  shopNumber?: string
  name: string
  source: CouponSource
  target: CouponTarget
  goods?: string
  type: CouponType
  validStartTime: string
  validEndTime: string
  stock: number
  receiveRule: string
  consumeRule: string
  status?: CouponStatus
}

export interface CouponTemplateDetailQuery {
  shopNumber: string
  couponTemplateId: string
}

export interface CouponRedeemPayload {
  source: number
  shopNumber: string
  couponTemplateId: string
}

export interface IncreaseCouponTemplatePayload {
  couponTemplateId: string
  number: number
}

export interface CouponTaskQuery {
  current: number
  size: number
  batchId?: string
  taskName?: string
  couponTemplateId?: string
  status?: CouponTaskStatus
}

export interface CouponTaskCreatePayload {
  taskName: string
  fileAddress: string
  notifyType: string
  couponTemplateId: string
  sendType: CouponTaskSendType
  sendTime?: string
}

export interface CouponTask {
  id?: string
  batchId?: string
  taskName: string
  fileAddress?: string
  failFileAddress?: string
  sendNum?: number
  notifyType: string
  couponTemplateId: string
  sendType: CouponTaskSendType
  sendTime?: string
  status?: CouponTaskStatus
  completionTime?: string
  operatorId?: string | number
}

export interface UserCouponQuery {
  current: number
  size: number
  status?: UserCouponStatus
}

export interface UserCoupon {
  id: string
  couponTemplateId: string
  userId: string
  receiveCount?: number
  source?: number
  status: UserCouponStatus
  receiveTime?: string
  validStartTime?: string
  validEndTime?: string
  useTime?: string
  name?: string
  shopNumber?: string
  target?: CouponTarget
  goods?: string
  type?: CouponType
  receiveRule?: string
  consumeRule?: string
}

export interface CouponRemindCreatePayload {
  couponTemplateId: string
  name?: string
  shopNumber: string
  userId: string
  contact?: string
  type: number
  remindTime: number
  startTime: string
}

export interface CouponRemindQuery {
  userId: string
  current?: number
  size?: number
}

export interface CouponRemindCancelPayload {
  couponTemplateId: string
  userId: string
  remindTime: number
  type: number
}

export interface CouponRemind {
  id: string
  shopNumber: string
  name: string
  source?: CouponSource
  target?: CouponTarget
  goods?: string
  type?: CouponType
  validStartTime: string
  validEndTime: string
  receiveRule?: string
  consumeRule?: string
  remindTime: string[]
  remindType: string[]
}

export interface SettlementGoods {
  goodsNumber: string
  goodsAmount: number
}

export interface SettlementCouponQueryPayload {
  orderAmount: number
  shopNumber: string
  goodsList: SettlementGoods[]
}

export interface SettlementCoupon {
  id: string
  target?: CouponTarget
  goods?: string
  type: CouponType
  consumeRule?: string
  couponAmount?: number | string
}

export interface SettlementCouponQueryResult {
  availableCoupons: SettlementCoupon[]
  notAvailableCoupons: SettlementCoupon[]
}

export interface ApplyCouponPayload {
  userId: string | number
  shopNumber: string | number
  orderAmount: number
  orderId: string | number
}

export interface ApplyCouponResult {
  orderId: string | number
  originalAmount: number | string
  finalAmount: number | string
  appliedCouponId: string | number
}

export interface ReceiveRuleDraft {
  limitPerPerson: number
  usageInstructions: string
  distributionMode?: DistributionMode
  applicableMerchantScope?: ApplicableMerchantScope
  applicableShopNumbers?: string
}

export interface ConsumeRuleDraft {
  termsOfUse?: number
  maximumDiscountAmount?: number
  discountRate?: number
  explanationOfUnmetConditions: string
}
