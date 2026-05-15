<template>
  <section class="product-page">
    <div class="product-hero">
      <div class="product-hero__copy">
        <p class="eyebrow">Boutique Market</p>
        <h1>商品商城</h1>
        <p>精选店铺好物与可叠加优惠券，先选商品，再进入购物车统一结算。</p>
      </div>
      <div class="product-hero__search">
        <el-input
          v-model.trim="filters.name"
          size="large"
          placeholder="搜索商品名称"
          clearable
          @keyup.enter="searchGoods"
        />
        <el-button type="primary" size="large" :icon="Search" @click="searchGoods">搜索</el-button>
      </div>
    </div>

    <div class="product-workspace">
      <aside class="product-filter-panel">
        <div class="product-filter-panel__head">
          <p class="eyebrow">Filter</p>
          <h3>筛选</h3>
        </div>

        <el-form :model="filters" label-position="top">
          <el-form-item label="商品分类">
            <el-cascader
              v-model="filters.categoryId"
              :options="categoryTree"
              :props="categoryCascaderProps"
              placeholder="全部分类"
              clearable
              filterable
            />
          </el-form-item>
          <el-form-item label="价格区间">
            <div class="price-range">
              <el-input-number v-model="filters.minPrice" :min="0" :precision="2" controls-position="right" placeholder="最低价" />
              <span>-</span>
              <el-input-number v-model="filters.maxPrice" :min="0" :precision="2" controls-position="right" placeholder="最高价" />
            </div>
          </el-form-item>
          <el-form-item label="店铺编号">
            <el-input v-model.trim="filters.shopNumber" placeholder="指定店铺" clearable />
          </el-form-item>
          <el-form-item label="排序">
            <el-segmented v-model="filters.sort" :options="sortOptions" />
          </el-form-item>
        </el-form>

        <div class="product-filter-panel__actions">
          <el-button :icon="RefreshCw" @click="resetFilters">重置</el-button>
          <el-button type="primary" :icon="SlidersHorizontal" @click="searchGoods">筛选</el-button>
        </div>
      </aside>

      <main class="product-results">
        <div class="product-results__bar">
          <div>
            <p class="eyebrow">Products</p>
            <h3>{{ resultTitle }}</h3>
          </div>
          <div class="product-results__meta">
            <el-tag>{{ pagination.total }} 件商品</el-tag>
            <el-button :icon="RefreshCw" @click="loadGoods">刷新</el-button>
          </div>
        </div>

        <div v-loading="loading" class="product-grid" data-testid="product-grid">
          <article
            v-for="goods in goodsList"
            :key="`${goods.shopNumber}-${goods.id}`"
            class="product-card"
            :data-testid="`product-card-${goods.id}`"
          >
            <button class="product-card__image" type="button" @click="openDetail(goods)">
              <img v-if="goods.mainImage" :src="goods.mainImage" :alt="goods.name" @error="onImageError" />
              <PackageOpen v-else :size="34" />
              <span v-if="discountText(goods)" class="product-card__discount">{{ discountText(goods) }}</span>
            </button>
            <div class="product-card__body">
              <button class="product-card__title" type="button" @click="openDetail(goods)">{{ goods.name }}</button>
              <div class="product-card__meta">
                <span>{{ goods.categoryName || '精选商品' }}</span>
                <span>店铺 {{ goods.shopNumber }}</span>
              </div>
              <div class="product-card__price-row">
                <strong>¥{{ money(goods.price) }}</strong>
                <span v-if="goods.originalPrice && goods.originalPrice > goods.price">¥{{ money(goods.originalPrice) }}</span>
              </div>
              <div class="product-card__foot">
                <span>库存 {{ goods.stock }}</span>
                <el-button
                  type="primary"
                  :icon="ShoppingCart"
                  :loading="isAdding(goods)"
                  :disabled="goods.stock <= 0"
                  :data-testid="`add-to-cart-${goods.id}`"
                  @click="addToCart(goods)"
                >
                  加购
                </el-button>
              </div>
            </div>
          </article>

          <el-empty v-if="!loading && goodsList.length === 0" description="暂无匹配商品" />
        </div>

        <div v-if="goodsList.length" class="product-pagination">
          <el-pagination
            v-model:current-page="pagination.current"
            v-model:page-size="pagination.size"
            :total="pagination.total"
            :page-sizes="[8, 12, 24, 36]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadGoods"
            @current-change="loadGoods"
          />
        </div>
      </main>
    </div>

    <el-drawer v-model="detailVisible" title="商品详情" size="560px">
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <div v-else-if="selectedGoods" class="product-detail">
        <div class="product-detail__media">
          <img v-if="detailMainImage" :src="detailMainImage" :alt="selectedGoods.name" @error="onImageError" />
          <PackageOpen v-else :size="42" />
        </div>
        <div class="product-detail__head">
          <el-tag>{{ selectedGoods.categoryName || '精选商品' }}</el-tag>
          <h3>{{ selectedGoods.name }}</h3>
          <p>{{ selectedGoods.description || '暂无商品描述' }}</p>
        </div>
        <div class="product-detail__price">
          <strong>¥{{ money(selectedGoods.price) }}</strong>
          <span v-if="selectedGoods.originalPrice && selectedGoods.originalPrice > selectedGoods.price">
            ¥{{ money(selectedGoods.originalPrice) }}
          </span>
        </div>
        <div class="product-detail__facts">
          <span>店铺 {{ selectedGoods.shopNumber }}</span>
          <span>库存 {{ selectedGoods.stock }} {{ selectedGoods.unit || '件' }}</span>
          <span>销量 {{ selectedGoods.sales || 0 }}</span>
        </div>
        <div v-if="selectedGoods.attributeValues?.length" class="product-detail__attrs">
          <el-tag v-for="item in selectedGoods.attributeValues" :key="`${item.attributeId}-${item.attributeValue}`" effect="plain">
            {{ item.attributeName || item.attributeId }}：{{ item.attributeValue }}
          </el-tag>
        </div>
        <div v-if="selectedGoods.images?.length" class="product-detail__thumbs">
          <button
            v-for="image in selectedGoods.images"
            :key="image.id || image.imageUrl"
            type="button"
            :class="{ 'is-active': image.imageUrl === detailMainImage }"
            @click="detailImage = image.imageUrl"
          >
            <img :src="image.imageUrl" :alt="selectedGoods.name" @error="onImageError" />
          </button>
        </div>
        <div class="product-detail__action">
          <el-input-number
            v-model="detailQuantity"
            :min="1"
            :max="Math.min(selectedGoods.stock || 1, 999)"
            :step="1"
            step-strictly
            controls-position="right"
          />
          <el-button
            type="primary"
            size="large"
            :icon="ShoppingCart"
            :loading="isAdding(selectedGoods)"
            :disabled="selectedGoods.stock <= 0"
            :data-testid="`detail-add-to-cart-${selectedGoods.id}`"
            @click="addToCart(selectedGoods, detailQuantity)"
          >
            加入购物车
          </el-button>
        </div>
      </div>
      <el-empty v-else description="未查询到商品" />
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { PackageOpen, RefreshCw, Search, ShoppingCart, SlidersHorizontal } from 'lucide-vue-next'
import { userGoodsApi } from '@/api/goods'
import { useCartStore } from '@/stores/cart'
import type { GoodsCategory, GoodsDetail, GoodsPageItem, GoodsSort } from '@/types/goods'

const cartStore = useCartStore()

const categoryCascaderProps = {
  value: 'id',
  label: 'name',
  children: 'children',
  emitPath: false,
  checkStrictly: true
}

const sortOptions: Array<{ label: string; value: GoodsSort }> = [
  { label: '推荐', value: 'recommend' },
  { label: '低价', value: 'priceAsc' },
  { label: '高价', value: 'priceDesc' },
  { label: '热销', value: 'salesDesc' },
  { label: '上新', value: 'newest' }
]

const filters = reactive<{
  name: string
  categoryId: string
  shopNumber: string
  minPrice?: number
  maxPrice?: number
  sort: GoodsSort
}>({
  name: '',
  categoryId: '',
  shopNumber: '',
  minPrice: undefined,
  maxPrice: undefined,
  sort: 'recommend'
})

const pagination = reactive({
  current: 1,
  size: 12,
  total: 0
})

const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const goodsList = ref<GoodsPageItem[]>([])
const categoryTree = ref<GoodsCategory[]>([])
const selectedGoods = ref<GoodsDetail | null>(null)
const detailImage = ref('')
const detailQuantity = ref(1)
const addingIds = ref<Set<string>>(new Set())

const resultTitle = computed(() => {
  const categoryName = categoryNameById(filters.categoryId)
  return categoryName ? `${categoryName} · 商品` : '全部商品'
})

const detailMainImage = computed(() => detailImage.value || selectedGoods.value?.mainImage || selectedGoods.value?.images?.[0]?.imageUrl || '')

async function loadCategories() {
  categoryTree.value = await userGoodsApi.categoryTree()
}

async function loadGoods() {
  loading.value = true
  try {
    const page = await userGoodsApi.page({
      current: pagination.current,
      size: pagination.size,
      name: filters.name || undefined,
      categoryId: filters.categoryId || undefined,
      shopNumber: filters.shopNumber || undefined,
      minPrice: filters.minPrice,
      maxPrice: filters.maxPrice,
      sort: filters.sort
    })
    goodsList.value = page.records || []
    pagination.total = Number(page.total || 0)
    pagination.current = Number(page.current || pagination.current)
    pagination.size = Number(page.size || pagination.size)
  } finally {
    loading.value = false
  }
}

function searchGoods() {
  pagination.current = 1
  void loadGoods()
}

function resetFilters() {
  filters.name = ''
  filters.categoryId = ''
  filters.shopNumber = ''
  filters.minPrice = undefined
  filters.maxPrice = undefined
  filters.sort = 'recommend'
  pagination.current = 1
  void loadGoods()
}

async function openDetail(goods: GoodsPageItem) {
  detailVisible.value = true
  detailLoading.value = true
  selectedGoods.value = null
  detailImage.value = ''
  detailQuantity.value = 1
  try {
    selectedGoods.value = await userGoodsApi.detail({
      goodsId: String(goods.id),
      shopNumber: goods.shopNumber
    })
  } finally {
    detailLoading.value = false
  }
}

async function addToCart(goods: GoodsPageItem | GoodsDetail, quantity = 1) {
  if (goods.stock <= 0) {
    ElMessage.warning('该商品暂无库存')
    return
  }
  const key = goodsKey(goods)
  addingIds.value = new Set(addingIds.value).add(key)
  try {
    await cartStore.addToCart(String(goods.id), Number(goods.shopNumber), quantity)
  } finally {
    const next = new Set(addingIds.value)
    next.delete(key)
    addingIds.value = next
  }
}

function isAdding(goods: GoodsPageItem | GoodsDetail) {
  return addingIds.value.has(goodsKey(goods))
}

function goodsKey(goods: GoodsPageItem | GoodsDetail) {
  return `${goods.shopNumber}-${goods.id}`
}

function money(value?: number) {
  return Number(value || 0).toFixed(2)
}

function discountText(goods: GoodsPageItem | GoodsDetail) {
  if (!goods.originalPrice || goods.originalPrice <= goods.price) return ''
  const rate = (Number(goods.price || 0) / Number(goods.originalPrice || 1)) * 10
  return `${rate.toFixed(1)}折`
}

function categoryNameById(categoryId?: string | number) {
  const id = String(categoryId || '')
  if (!id) return ''
  return flattenCategories(categoryTree.value).find((category) => String(category.id) === id)?.name || ''
}

function flattenCategories(tree: GoodsCategory[]): GoodsCategory[] {
  return tree.flatMap((category) => [category, ...flattenCategories(category.children || [])])
}

function onImageError(event: Event) {
  const image = event.target as HTMLImageElement
  image.style.display = 'none'
}

onMounted(async () => {
  await loadCategories()
  await loadGoods()
})
</script>

<style scoped>
.product-page {
  display: grid;
  gap: 22px;
  padding: 28px;
}

.product-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 0.7fr);
  gap: 22px;
  align-items: end;
  min-height: 230px;
  overflow: hidden;
  padding: clamp(28px, 5vw, 48px);
  border: 1px solid rgba(137, 29, 46, 0.16);
  border-radius: 8px;
  color: #fff;
  background:
    linear-gradient(120deg, rgba(32, 29, 26, 0.88), rgba(94, 26, 42, 0.78)),
    url("https://images.unsplash.com/photo-1441986300917-64674bd600d8?auto=format&fit=crop&w=1800&q=80") center/cover;
  box-shadow: 0 22px 52px rgba(48, 31, 28, 0.18);
}

.product-hero__copy {
  max-width: 680px;
}

.product-hero h1 {
  margin: 0;
  font-size: clamp(34px, 5vw, 62px);
  line-height: 1.08;
  letter-spacing: 0;
}

.product-hero p:not(.eyebrow) {
  max-width: 560px;
  margin: 14px 0 0;
  color: rgba(255, 255, 255, 0.84);
  font-size: 16px;
  line-height: 1.8;
}

.product-hero__search {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  padding: 10px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(14px);
}

.product-workspace {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.product-filter-panel,
.product-results__bar,
.product-pagination {
  border: 1px solid rgba(228, 221, 211, 0.92);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12px 34px rgba(47, 38, 31, 0.06);
}

.product-filter-panel {
  position: sticky;
  top: 92px;
  display: grid;
  gap: 18px;
  padding: 20px;
}

.product-filter-panel__head h3,
.product-results__bar h3 {
  margin: 0;
}

.product-filter-panel :deep(.el-cascader),
.product-filter-panel :deep(.el-segmented) {
  width: 100%;
}

.price-range {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  gap: 8px;
  align-items: center;
}

.price-range :deep(.el-input-number) {
  width: 100%;
}

.product-filter-panel__actions,
.product-results__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.product-results {
  display: grid;
  gap: 16px;
}

.product-results__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 18px 20px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  min-height: 360px;
}

.product-grid :deep(.el-empty) {
  grid-column: 1 / -1;
}

.product-card {
  overflow: hidden;
  border: 1px solid rgba(228, 221, 211, 0.92);
  border-radius: 8px;
  background: #fffdf9;
  box-shadow: 0 12px 30px rgba(47, 38, 31, 0.06);
  transition:
    border-color var(--duration-normal) var(--ease-out),
    box-shadow var(--duration-normal) var(--ease-out),
    transform var(--duration-normal) var(--ease-out);
}

.product-card:hover {
  border-color: rgba(137, 29, 46, 0.28);
  box-shadow: 0 20px 48px rgba(64, 40, 36, 0.12);
  transform: translateY(-2px);
}

.product-card__image {
  position: relative;
  display: grid;
  width: 100%;
  aspect-ratio: 1 / 0.78;
  place-items: center;
  overflow: hidden;
  border: 0;
  color: var(--user-muted, #7c746b);
  background:
    linear-gradient(135deg, rgba(248, 244, 236, 0.96), rgba(238, 233, 224, 0.86));
  cursor: pointer;
}

.product-card__image img,
.product-detail__media img,
.product-detail__thumbs img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-card__discount {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 4px 8px;
  border-radius: 999px;
  color: #fff;
  background: #8f2035;
  font-size: 12px;
  font-weight: 900;
}

.product-card__body {
  display: grid;
  gap: 10px;
  padding: 14px;
}

.product-card__title {
  display: -webkit-box;
  min-height: 44px;
  overflow: hidden;
  padding: 0;
  border: 0;
  color: #211d1b;
  background: transparent;
  font-size: 15px;
  font-weight: 900;
  line-height: 1.45;
  text-align: left;
  cursor: pointer;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.product-card__meta,
.product-card__foot,
.product-detail__facts {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: #7c746b;
  font-size: 12px;
}

.product-card__price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.product-card__price-row strong,
.product-detail__price strong {
  color: #8f2035;
  font-family: var(--font-family-number);
  font-size: 24px;
}

.product-card__price-row span,
.product-detail__price span {
  color: #9b9388;
  text-decoration: line-through;
}

.product-card__foot {
  flex-wrap: nowrap;
}

.product-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 14px;
}

.product-detail {
  display: grid;
  gap: 18px;
}

.product-detail__media {
  display: grid;
  aspect-ratio: 1 / 0.72;
  place-items: center;
  overflow: hidden;
  border: 1px solid rgba(228, 221, 211, 0.92);
  border-radius: 8px;
  color: #7c746b;
  background: #f6f1e8;
}

.product-detail__head {
  display: grid;
  gap: 10px;
}

.product-detail__head h3 {
  margin: 0;
  color: #211d1b;
  font-size: 24px;
  line-height: 1.35;
}

.product-detail__head p {
  margin: 0;
  color: #6f675f;
  line-height: 1.8;
}

.product-detail__price {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.product-detail__price strong {
  font-size: 34px;
}

.product-detail__facts {
  justify-content: flex-start;
  padding: 12px;
  border: 1px solid rgba(228, 221, 211, 0.9);
  border-radius: 8px;
  background: #fffaf2;
}

.product-detail__attrs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.product-detail__thumbs {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.product-detail__thumbs button {
  aspect-ratio: 1;
  overflow: hidden;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 8px;
  background: #f6f1e8;
  cursor: pointer;
}

.product-detail__thumbs button.is-active {
  border-color: #8f2035;
}

.product-detail__action {
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr);
  gap: 10px;
}

.product-detail__action :deep(.el-input-number) {
  width: 100%;
}

@media (max-width: 1280px) {
  .product-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 980px) {
  .product-page {
    padding: 18px;
  }

  .product-hero,
  .product-workspace {
    grid-template-columns: 1fr;
  }

  .product-filter-panel {
    position: static;
  }

  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 620px) {
  .product-page {
    padding: 14px 14px 88px;
  }

  .product-hero {
    min-height: 0;
    padding: 24px;
  }

  .product-hero__search,
  .price-range,
  .product-results__bar,
  .product-detail__action {
    grid-template-columns: 1fr;
  }

  .product-results__bar {
    align-items: stretch;
  }

  .product-results__meta {
    justify-content: flex-start;
  }

  .product-grid {
    grid-template-columns: 1fr;
  }
}
</style>
