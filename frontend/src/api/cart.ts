import { http } from './http'
import type { CartAddParams, CartSelectParams, CartSummary, CartUpdateQuantityParams } from '@/types/cart'

export const cartApi = {
  getSummary() {
    return http.get<unknown, CartSummary>('/api/engine/cart/summary')
  },

  addToCart(data: CartAddParams) {
    return http.post<unknown, void>('/api/engine/cart/add', data)
  },

  updateQuantity(data: CartUpdateQuantityParams) {
    return http.post<unknown, void>('/api/engine/cart/update-quantity', data)
  },

  removeItem(cartId: string) {
    return http.post<unknown, void>('/api/engine/cart/remove', null, { params: { cartId } })
  },

  removeBatch(cartIds: string[]) {
    return http.post<unknown, void>('/api/engine/cart/remove-batch', cartIds)
  },

  clearCart() {
    return http.post<unknown, void>('/api/engine/cart/clear')
  },

  updateSelected(data: CartSelectParams) {
    return http.post<unknown, void>('/api/engine/cart/select', data)
  }
}
