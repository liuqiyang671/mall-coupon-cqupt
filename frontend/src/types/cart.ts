export interface CartItem {
  id: string
  goodsId: string
  shopNumber: number
  goodsName: string
  mainImage: string
  price: number
  originalPrice: number
  quantity: number
  subtotal: number
  selected: 0 | 1
  goodsStatus: number
  goodsStock: number
  createTime: string
}

export interface CartSummary {
  items: CartItem[]
  totalCount: number
  selectedCount: number
  totalAmount: number
  selectedAmount: number
  savedAmount: number
  taxAmount: number
  payableAmount: number
}

export interface CartAddParams {
  goodsId: string
  shopNumber: number
  quantity?: number
}

export interface CartUpdateQuantityParams {
  cartId: string
  quantity: number
}

export interface CartSelectParams {
  cartIds?: string[]
  selected: 0 | 1
}
