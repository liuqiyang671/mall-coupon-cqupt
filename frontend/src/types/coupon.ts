export type CouponSource = 0 | 1
export type CouponTarget = 0 | 1
export type CouponType = 0 | 1 | 2
export type CouponStatus = 0 | 1
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

export interface IncreaseCouponTemplatePayload {
  couponTemplateId: string
  number: number
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
