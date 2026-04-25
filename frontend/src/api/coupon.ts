import { http } from './http'
import type {
  ApplyCouponPayload,
  ApplyCouponResult,
  CouponCenterQuery,
  CouponRemind,
  CouponRemindCancelPayload,
  CouponRemindCreatePayload,
  CouponRemindQuery,
  CouponRedeemPayload,
  CouponTask,
  CouponTaskCreatePayload,
  CouponTaskQuery,
  CouponTemplate,
  CouponTemplateDetailQuery,
  CouponTemplateQuery,
  CouponTemplateSavePayload,
  IncreaseCouponTemplatePayload,
  PageResult,
  SettlementCouponQueryPayload,
  SettlementCouponQueryResult,
  UserCoupon,
  UserCouponQuery
} from '@/types/coupon'

export const couponTemplateApi = {
  page(params: CouponTemplateQuery) {
    return http.get<unknown, PageResult<CouponTemplate>>('/api/merchant-admin/coupon-template/page', { params })
  },

  detail(couponTemplateId: string) {
    return http.get<unknown, CouponTemplate>('/api/merchant-admin/coupon-template/find', {
      params: { couponTemplateId }
    })
  },

  create(payload: CouponTemplateSavePayload) {
    return http.post<unknown, void>('/api/merchant-admin/coupon-template/save', payload)
  },

  update(couponTemplateId: string, payload: CouponTemplateSavePayload) {
    return http.post<unknown, void>('/api/merchant-admin/coupon-template/update', payload, {
      params: { couponTemplateId }
    })
  },

  terminate(couponTemplateId: string) {
    return http.post<unknown, void>('/api/merchant-admin/coupon-template/terminate', undefined, {
      params: { couponTemplateId }
    })
  },

  increaseNumber(payload: IncreaseCouponTemplatePayload) {
    return http.post<unknown, void>('/api/merchant-admin/coupon-template/increase-number', payload)
  }
}

export const couponTaskApi = {
  page(params: CouponTaskQuery) {
    return http.get<unknown, PageResult<CouponTask>>('/api/merchant-admin/coupon-task/page', { params })
  },

  detail(taskId: string) {
    return http.get<unknown, CouponTask>('/api/merchant-admin/coupon-task/find', {
      params: { taskId }
    })
  },

  create(payload: CouponTaskCreatePayload) {
    return http.post<unknown, void>('/api/merchant-admin/coupon-task/create', payload)
  }
}

export const couponCenterApi = {
  page(params: CouponCenterQuery) {
    return http.get<unknown, PageResult<CouponTemplate>>('/api/engine/coupon-template/page', { params })
  },

  detail(params: CouponTemplateDetailQuery) {
    return http.get<unknown, CouponTemplate>('/api/engine/coupon-template/query', { params })
  },

  redeem(payload: CouponRedeemPayload) {
    return http.post<unknown, void>('/api/engine/user-coupon/redeem', payload)
  },

  redeemByMq(payload: CouponRedeemPayload) {
    return http.post<unknown, void>('/api/engine/user-coupon/redeem-mq', payload)
  }
}

export const userCouponApi = {
  page(params: UserCouponQuery) {
    return http.get<unknown, PageResult<UserCoupon>>('/api/engine/user-coupon/page', { params })
  }
}

export const couponRemindApi = {
  list(params: CouponRemindQuery) {
    return http.get<unknown, CouponRemind[]>('/api/engine/coupon-template-remind/list', { params })
  },

  create(payload: CouponRemindCreatePayload) {
    return http.post<unknown, boolean>('/api/engine/coupon-template-remind/create', payload)
  },

  cancel(payload: CouponRemindCancelPayload) {
    return http.post<unknown, boolean>('/api/engine/coupon-template-remind/cancel', payload)
  }
}

export const settlementApi = {
  queryCoupons(payload: SettlementCouponQueryPayload) {
    return http.post<unknown, SettlementCouponQueryResult>('/api/settlement/coupon-query', payload)
  },

  queryCouponsSync(payload: SettlementCouponQueryPayload) {
    return http.post<unknown, SettlementCouponQueryResult>('/api/settlement/coupon-query-sync', payload)
  },

  applyCoupon(couponId: string, payload: ApplyCouponPayload) {
    return http.post<unknown, ApplyCouponResult>(`/api/settlement/apply-coupon/${couponId}`, payload)
  }
}
