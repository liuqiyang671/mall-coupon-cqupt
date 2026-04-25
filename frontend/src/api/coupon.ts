import { http } from './http'
import type {
  CouponTemplate,
  CouponTemplateQuery,
  CouponTemplateSavePayload,
  IncreaseCouponTemplatePayload,
  PageResult
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
