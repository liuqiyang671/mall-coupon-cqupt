import { http } from './http'
import type {
  GoodsAttribute,
  GoodsAttributeSavePayload,
  GoodsCategory,
  GoodsCategorySavePayload,
  GoodsDetail,
  GoodsPageItem,
  GoodsPageQuery,
  GoodsPageResult,
  GoodsSavePayload,
  GoodsStatus,
  GoodsStockPayload,
  UserGoodsDetailQuery,
  UserGoodsPageQuery
} from '@/types/goods'

export const goodsApi = {
  page(params: GoodsPageQuery) {
    return http.get<unknown, GoodsPageResult>('/api/merchant-admin/goods/page', { params })
  },

  detail(goodsId: string) {
    return http.get<unknown, GoodsDetail>('/api/merchant-admin/goods/find', {
      params: { goodsId }
    })
  },

  create(payload: GoodsSavePayload) {
    return http.post<unknown, void>('/api/merchant-admin/goods/save', payload)
  },

  update(goodsId: string, payload: GoodsSavePayload) {
    return http.post<unknown, void>('/api/merchant-admin/goods/update', payload, {
      params: { goodsId }
    })
  },

  updateStatus(goodsId: string, status: GoodsStatus) {
    return http.post<unknown, void>('/api/merchant-admin/goods/update-status', undefined, {
      params: { goodsId, status }
    })
  },

  adjustStock(payload: GoodsStockPayload) {
    return http.post<unknown, void>('/api/merchant-admin/goods/adjust-stock', payload)
  },

  delete(goodsId: string) {
    return http.post<unknown, void>('/api/merchant-admin/goods/delete', undefined, {
      params: { goodsId }
    })
  },

  listByIds(goodsIds: string[]) {
    return http.post<unknown, GoodsPageItem[]>('/api/merchant-admin/goods/list-by-ids', goodsIds)
  }
}

export const goodsCategoryApi = {
  tree() {
    return http.get<unknown, GoodsCategory[]>('/api/merchant-admin/goods-category/tree')
  },

  create(payload: GoodsCategorySavePayload) {
    return http.post<unknown, void>('/api/merchant-admin/goods-category/save', payload)
  },

  update(categoryId: string, payload: GoodsCategorySavePayload) {
    return http.post<unknown, void>('/api/merchant-admin/goods-category/update', payload, {
      params: { categoryId }
    })
  },

  delete(categoryId: string) {
    return http.post<unknown, void>('/api/merchant-admin/goods-category/delete', undefined, {
      params: { categoryId }
    })
  },

  updateStatus(categoryId: string, status: 0 | 1) {
    return http.post<unknown, void>('/api/merchant-admin/goods-category/update-status', undefined, {
      params: { categoryId, status }
    })
  }
}

export const userGoodsApi = {
  page(params: UserGoodsPageQuery) {
    return http.get<unknown, GoodsPageResult>('/api/user/goods/page', { params })
  },

  detail(params: UserGoodsDetailQuery) {
    return http.get<unknown, GoodsDetail>('/api/user/goods/find', { params })
  },

  categoryTree() {
    return http.get<unknown, GoodsCategory[]>('/api/user/goods-category/tree')
  }
}

export const goodsAttributeApi = {
  list() {
    return http.get<unknown, GoodsAttribute[]>('/api/merchant-admin/goods-attribute/list')
  },

  create(payload: GoodsAttributeSavePayload) {
    return http.post<unknown, void>('/api/merchant-admin/goods-attribute/save', payload)
  },

  update(attributeId: string, payload: GoodsAttributeSavePayload) {
    return http.post<unknown, void>('/api/merchant-admin/goods-attribute/update', payload, {
      params: { attributeId }
    })
  },

  delete(attributeId: string) {
    return http.post<unknown, void>('/api/merchant-admin/goods-attribute/delete', undefined, {
      params: { attributeId }
    })
  }
}
