import type { PageResult } from '@/types/coupon'

export type GoodsStatus = 0 | 1 | 2
export type CategoryStatus = 0 | 1
export type AttributeInputType = 0 | 1 | 2

export interface GoodsPageQuery {
  current: number
  size: number
  name?: string
  categoryId?: string | number
  status?: GoodsStatus
  minPrice?: number
  maxPrice?: number
}

export type GoodsSort = 'recommend' | 'priceAsc' | 'priceDesc' | 'salesDesc' | 'newest'

export interface UserGoodsPageQuery {
  current: number
  size: number
  name?: string
  categoryId?: string | number
  shopNumber?: string | number
  minPrice?: number
  maxPrice?: number
  sort?: GoodsSort
}

export interface UserGoodsDetailQuery {
  goodsId: string
  shopNumber: string | number
}

export interface GoodsPageItem {
  id: string
  shopNumber: string
  categoryId: string
  categoryName?: string
  name: string
  mainImage?: string
  price: number
  originalPrice?: number
  stock: number
  sales?: number
  status: GoodsStatus
  createTime?: string
}

export interface GoodsImage {
  id?: string
  imageUrl: string
  sortOrder?: number
}

export interface GoodsAttributeValue {
  attributeId: string
  attributeName?: string
  attributeValue: string
}

export interface GoodsDetail extends GoodsPageItem {
  description?: string
  unit?: string
  sortOrder?: number
  images?: GoodsImage[]
  attributeValues?: GoodsAttributeValue[]
}

export interface GoodsSavePayload {
  name: string
  categoryId: string | number
  description?: string
  mainImage?: string
  price: number
  originalPrice?: number
  stock: number
  unit?: string
  sortOrder?: number
  imageUrls?: string[]
  attributeValues?: Array<{
    attributeId: string | number
    attributeValue: string
  }>
}

export interface GoodsStockPayload {
  goodsId: string | number
  quantity: number
}

export interface GoodsCategory {
  id: string
  parentId: string
  name: string
  icon?: string
  sortOrder?: number
  level?: number
  status: CategoryStatus
  createTime?: string
  children?: GoodsCategory[]
}

export interface GoodsCategorySavePayload {
  parentId: string | number
  name: string
  icon?: string
  sortOrder?: number
}

export interface GoodsAttribute {
  id: string
  name: string
  inputType: AttributeInputType
  values?: string
  sortOrder?: number
  status: CategoryStatus
}

export interface GoodsAttributeSavePayload {
  name: string
  inputType: AttributeInputType
  values?: string
  sortOrder?: number
}

export type GoodsPageResult = PageResult<GoodsPageItem>
