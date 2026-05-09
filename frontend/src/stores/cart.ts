import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { cartApi } from '@/api/cart'
import type { CartItem, CartSummary } from '@/types/cart'
import { ElMessage, ElMessageBox } from 'element-plus'

export const useCartStore = defineStore('cart', () => {
  const summary = ref<CartSummary | null>(null)
  const loading = ref(false)
  const initialized = ref(false)
  let summaryRequestSeq = 0

  const items = computed(() => summary.value?.items ?? [])
  const totalCount = computed(() => summary.value?.totalCount ?? 0)
  const selectedCount = computed(() => summary.value?.selectedCount ?? 0)
  const selectedAmount = computed(() => summary.value?.selectedAmount ?? 0)
  const savedAmount = computed(() => summary.value?.savedAmount ?? 0)
  const taxAmount = computed(() => summary.value?.taxAmount ?? 0)
  const payableAmount = computed(() => summary.value?.payableAmount ?? 0)
  const totalAmount = computed(() => summary.value?.totalAmount ?? 0)
  const isEmpty = computed(() => items.value.length === 0)
  const isAllSelected = computed(() => {
    const validItems = items.value.filter(i => i.goodsStatus === 1)
    return validItems.length > 0 && validItems.every(i => i.selected === 1)
  })

  async function fetchSummary() {
    const requestSeq = ++summaryRequestSeq
    loading.value = true
    try {
      const nextSummary = await cartApi.getSummary()
      if (requestSeq === summaryRequestSeq) {
        summary.value = nextSummary
        initialized.value = true
      }
    } catch (e: any) {
      if (requestSeq === summaryRequestSeq && e?.code !== '401') {
        console.error('获取购物车失败:', e)
      }
    } finally {
      if (requestSeq === summaryRequestSeq) {
        loading.value = false
      }
    }
  }

  async function addToCart(goodsId: string, shopNumber: number, quantity = 1) {
    try {
      await cartApi.addToCart({ goodsId, shopNumber, quantity })
      ElMessage.success('已加入购物车')
      await fetchSummary()
    } catch (e: any) {
      ElMessage.error(e?.message || '加入购物车失败')
    }
  }

  async function updateQuantity(cartId: string, quantity: number) {
    if (quantity < 1 || quantity > 999) return
    const item = items.value.find(i => i.id === cartId)
    if (!item) return

    const oldQuantity = item.quantity
    if (item.subtotal !== undefined && item.price !== undefined) {
      item.quantity = quantity
      item.subtotal = Number((item.price * quantity).toFixed(2))
    }

    try {
      await cartApi.updateQuantity({ cartId, quantity })
      await fetchSummary()
    } catch (e: any) {
      item.quantity = oldQuantity
      if (item.price !== undefined) {
        item.subtotal = Number((item.price * oldQuantity).toFixed(2))
      }
      ElMessage.error(e?.message || '修改数量失败')
    }
  }

  async function removeItem(cartId: string) {
    try {
      await ElMessageBox.confirm('确定要删除该商品吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await cartApi.removeItem(cartId)
      ElMessage.success('已删除')
      await fetchSummary()
    } catch {
      // cancelled
    }
  }

  async function removeSelected() {
    const selectedItems = items.value.filter(i => i.selected === 1)
    if (selectedItems.length === 0) {
      ElMessage.warning('请先选择商品')
      return
    }
    try {
      await ElMessageBox.confirm(`确定要删除选中的 ${selectedItems.length} 件商品吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await cartApi.removeBatch(selectedItems.map(i => i.id))
      ElMessage.success('已删除')
      await fetchSummary()
    } catch {
      // cancelled
    }
  }

  async function clearCart() {
    if (isEmpty.value) return
    try {
      await ElMessageBox.confirm('确定要清空购物车吗？此操作不可恢复', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await cartApi.clearCart()
      ElMessage.success('购物车已清空')
      await fetchSummary()
    } catch {
      // cancelled
    }
  }

  async function toggleSelect(cartId: string) {
    const item = items.value.find(i => i.id === cartId)
    if (!item) return
    const newSelected = item.selected === 1 ? 0 : 1
    try {
      await cartApi.updateSelected({ cartIds: [cartId], selected: newSelected as 0 | 1 })
      await fetchSummary()
    } catch (e: any) {
      ElMessage.error(e?.message || '操作失败')
    }
  }

  async function toggleSelectAll() {
    const newSelected = isAllSelected.value ? 0 : 1
    try {
      await cartApi.updateSelected({ selected: newSelected as 0 | 1 })
      await fetchSummary()
    } catch (e: any) {
      ElMessage.error(e?.message || '操作失败')
    }
  }

  function reset() {
    summaryRequestSeq += 1
    summary.value = null
    loading.value = false
    initialized.value = false
  }

  return {
    summary,
    loading,
    initialized,
    items,
    totalCount,
    selectedCount,
    selectedAmount,
    savedAmount,
    taxAmount,
    payableAmount,
    totalAmount,
    isEmpty,
    isAllSelected,
    fetchSummary,
    addToCart,
    updateQuantity,
    removeItem,
    removeSelected,
    clearCart,
    toggleSelect,
    toggleSelectAll,
    reset
  }
})
