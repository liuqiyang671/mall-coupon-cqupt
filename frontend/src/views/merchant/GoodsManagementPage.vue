<template>
  <PermissionGate :roles="[1]">
    <section class="coupon-page goods-page">
      <div class="surface-card coupon-toolbar">
        <div>
          <p class="eyebrow">Goods</p>
          <h3>商品管理</h3>
          <p class="muted">维护当前店铺商品、分类、图片、库存和上下架状态。</p>
        </div>
        <div class="toolbar-actions">
          <el-tag type="info">店铺 {{ authStore.shopNumber || '-' }}</el-tag>
          <el-button :icon="FolderPlus" @click="openCategoryDialog">新增分类</el-button>
          <el-button type="primary" :icon="Plus" @click="openCreateDrawer">新增商品</el-button>
        </div>
      </div>

      <div class="role-policy-grid">
        <article class="surface-card policy-card">
          <PackageCheck :size="20" />
          <div>
            <strong>商品资料</strong>
            <p>商品以当前商家店铺为边界，支持维护主图、价格、库存、分类、图片和属性。</p>
          </div>
        </article>
        <article class="surface-card policy-card">
          <ShieldCheck :size="20" />
          <div>
            <strong>上架规则</strong>
            <p>新建商品默认下架，确认资料和库存后再上架；已上架商品需先下架后删除。</p>
          </div>
        </article>
      </div>

      <div class="surface-card filter-panel">
        <el-form :model="filters" label-position="top">
          <div class="filter-grid">
            <el-form-item label="商品名称">
              <el-input v-model.trim="filters.name" placeholder="按商品名称搜索" clearable />
            </el-form-item>
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
            <el-form-item label="商品状态">
              <el-select v-model="filters.status" placeholder="全部状态" clearable>
                <el-option v-for="item in goodsStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="价格区间">
              <div class="price-range">
                <el-input-number v-model="filters.minPrice" :min="0" :precision="2" controls-position="right" placeholder="最低价" />
                <span>至</span>
                <el-input-number v-model="filters.maxPrice" :min="0" :precision="2" controls-position="right" placeholder="最高价" />
              </div>
            </el-form-item>
          </div>
          <div class="filter-actions">
            <el-button :icon="RefreshCw" @click="resetFilters">重置</el-button>
            <el-button type="primary" :icon="Search" @click="loadGoods">查询</el-button>
          </div>
        </el-form>
      </div>

      <div class="surface-card table-card">
        <el-table v-loading="loading" :data="goodsList" row-key="id" stripe>
          <el-table-column label="商品" min-width="260">
            <template #default="{ row }">
              <div class="goods-cell">
                <div class="goods-thumb">
                  <img v-if="row.mainImage" :src="row.mainImage" :alt="row.name" @error="onImageError" />
                  <Package v-else :size="24" />
                </div>
                <div class="goods-cell__text">
                  <strong>{{ row.name }}</strong>
                  <span>ID：{{ row.id }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="分类" min-width="140" show-overflow-tooltip>
            <template #default="{ row }">{{ row.categoryName || categoryNameById(row.categoryId) || '-' }}</template>
          </el-table-column>
          <el-table-column label="售价" width="130">
            <template #default="{ row }">
              <div class="price-stack">
                <strong>¥{{ money(row.price) }}</strong>
                <span v-if="row.originalPrice && row.originalPrice > row.price">¥{{ money(row.originalPrice) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="库存 / 销量" width="130">
            <template #default="{ row }">
              <div class="goods-stat">
                <el-tag :type="stockTagType(row.stock)" size="small">{{ row.stock }} 件</el-tag>
                <span>售 {{ row.sales || 0 }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="goodsStatusTagType(row.status)">{{ goodsStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" min-width="170" show-overflow-tooltip />
          <el-table-column label="操作" fixed="right" width="300">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="primary" @click="openStockDialog(row)">库存</el-button>
              <el-button
                v-if="row.status === 1"
                link
                type="warning"
                @click="changeStatus(row, 0)"
              >
                下架
              </el-button>
              <el-button
                v-else
                link
                type="success"
                :disabled="row.status === 2 || row.stock <= 0"
                @click="changeStatus(row, 1)"
              >
                上架
              </el-button>
              <el-button link type="danger" :disabled="row.status === 1" @click="deleteGoods(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-footer">
          <el-pagination
            v-model:current-page="pagination.current"
            v-model:page-size="pagination.size"
            :total="pagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadGoods"
            @current-change="loadGoods"
          />
        </div>
      </div>
    </section>

    <el-drawer v-model="formDrawerVisible" :title="formDrawerTitle" size="680px" destroy-on-close>
      <el-form ref="goodsFormRef" :model="goodsForm" :rules="goodsRules" label-position="top">
        <el-alert
          v-if="categoryTree.length === 0"
          title="请先创建商品分类"
          type="warning"
          show-icon
          :closable="false"
          class="form-alert"
        />

        <div class="form-section-title">基础信息</div>
        <el-form-item label="商品名称" prop="name">
          <el-input v-model.trim="goodsForm.name" placeholder="例如：重邮校园文创礼盒" clearable maxlength="80" show-word-limit />
        </el-form-item>
        <div class="two-column-form">
          <el-form-item label="商品分类" prop="categoryId">
            <el-cascader
              v-model="goodsForm.categoryId"
              :options="categoryTree"
              :props="categoryCascaderProps"
              placeholder="选择分类"
              clearable
              filterable
            />
          </el-form-item>
          <el-form-item label="计量单位">
            <el-input v-model.trim="goodsForm.unit" placeholder="件 / 份 / 套" clearable />
          </el-form-item>
        </div>
        <el-form-item label="商品描述">
          <el-input v-model.trim="goodsForm.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>

        <div class="form-section-title">价格与库存</div>
        <div class="two-column-form">
          <el-form-item label="售价" prop="price">
            <el-input-number v-model="goodsForm.price" :min="0.01" :precision="2" controls-position="right" />
          </el-form-item>
          <el-form-item label="原价">
            <el-input-number v-model="goodsForm.originalPrice" :min="0" :precision="2" controls-position="right" />
          </el-form-item>
          <el-form-item label="库存" prop="stock">
            <el-input-number v-model="goodsForm.stock" :min="0" :step="1" step-strictly controls-position="right" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="goodsForm.sortOrder" :min="0" :step="1" step-strictly controls-position="right" />
          </el-form-item>
        </div>

        <div class="form-section-title">图片</div>
        <el-form-item label="主图 URL">
          <el-input v-model.trim="goodsForm.mainImage" placeholder="https://..." clearable />
        </el-form-item>
        <div class="image-url-list">
          <el-form-item v-for="(_, index) in goodsForm.imageUrls" :key="index" :label="`图片 URL ${index + 1}`">
            <div class="image-url-row">
              <el-input v-model.trim="goodsForm.imageUrls[index]" placeholder="https://..." clearable />
              <el-button :icon="Trash2" circle :disabled="goodsForm.imageUrls.length === 1" @click="removeImageUrl(index)" />
            </div>
          </el-form-item>
          <el-button :icon="ImagePlus" @click="addImageUrl">添加图片</el-button>
        </div>

        <template v-if="attributes.length">
          <div class="form-section-title">商品属性</div>
          <div class="two-column-form">
            <el-form-item v-for="attribute in attributes" :key="attribute.id" :label="attribute.name">
              <el-select
                v-if="attribute.inputType === 1 && attributeOptions(attribute).length"
                v-model="goodsForm.attributes[attribute.id]"
                placeholder="请选择"
                clearable
              >
                <el-option v-for="option in attributeOptions(attribute)" :key="option" :label="option" :value="option" />
              </el-select>
              <el-input v-else v-model.trim="goodsForm.attributes[attribute.id]" clearable />
            </el-form-item>
          </div>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="formDrawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="categoryTree.length === 0" @click="submitGoods">
          {{ editingGoodsId ? '保存修改' : '提交创建' }}
        </el-button>
      </template>
    </el-drawer>

    <el-drawer v-model="detailDrawerVisible" title="商品详情" size="560px">
      <el-skeleton v-if="detailLoading" :rows="7" animated />
      <div v-else-if="selectedGoods" class="detail-panel">
        <div class="goods-detail-hero">
          <div class="goods-detail-hero__image">
            <img v-if="selectedGoods.mainImage" :src="selectedGoods.mainImage" :alt="selectedGoods.name" @error="onImageError" />
            <Package v-else :size="32" />
          </div>
          <div>
            <el-tag :type="goodsStatusTagType(selectedGoods.status)">{{ goodsStatusText(selectedGoods.status) }}</el-tag>
            <h3>{{ selectedGoods.name }}</h3>
            <p>ID：{{ selectedGoods.id }}</p>
          </div>
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="店铺编号">{{ selectedGoods.shopNumber }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ selectedGoods.categoryName || categoryNameById(selectedGoods.categoryId) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="价格">¥{{ money(selectedGoods.price) }}</el-descriptions-item>
          <el-descriptions-item label="原价">¥{{ money(selectedGoods.originalPrice || selectedGoods.price) }}</el-descriptions-item>
          <el-descriptions-item label="库存">{{ selectedGoods.stock }} {{ selectedGoods.unit || '件' }}</el-descriptions-item>
          <el-descriptions-item label="销量">{{ selectedGoods.sales || 0 }}</el-descriptions-item>
          <el-descriptions-item label="描述">{{ selectedGoods.description || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="selectedGoods.images?.length" class="goods-image-grid">
          <img v-for="image in selectedGoods.images" :key="image.id || image.imageUrl" :src="image.imageUrl" :alt="selectedGoods.name" @error="onImageError" />
        </div>
        <el-descriptions v-if="selectedGoods.attributeValues?.length" :column="1" border>
          <el-descriptions-item v-for="item in selectedGoods.attributeValues" :key="item.attributeId" :label="item.attributeName || item.attributeId">
            {{ item.attributeValue }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <el-empty v-else description="未查询到商品" />
    </el-drawer>

    <el-dialog v-model="stockDialogVisible" title="调整库存" width="420px">
      <el-form label-position="top">
        <el-form-item label="商品">
          <el-input :model-value="stockTarget?.name || '-'" disabled />
        </el-form-item>
        <el-form-item label="当前库存">
          <el-input :model-value="String(stockTarget?.stock ?? '-')" disabled />
        </el-form-item>
        <el-form-item label="调整数量">
          <el-input-number v-model="stockDelta" :min="-999999" :max="999999" :step="1" step-strictly controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="adjustingStock" @click="submitStock">确认调整</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="categoryDialogVisible" title="新增商品分类" width="460px">
      <el-form ref="categoryFormRef" :model="categoryForm" :rules="categoryRules" label-position="top">
        <el-form-item label="父级分类">
          <el-cascader
            v-model="categoryForm.parentId"
            :options="parentCategoryOptions"
            :props="categoryCascaderProps"
            placeholder="一级分类"
            clearable
            filterable
          />
        </el-form-item>
        <el-form-item label="分类名称" prop="name">
          <el-input v-model.trim="categoryForm.name" placeholder="例如：校园文创" clearable maxlength="40" />
        </el-form-item>
        <el-form-item label="分类图标 URL">
          <el-input v-model.trim="categoryForm.icon" placeholder="https://..." clearable />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="categoryForm.sortOrder" :min="0" :step="1" step-strictly controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingCategory" @click="submitCategory">保存分类</el-button>
      </template>
    </el-dialog>
  </PermissionGate>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { FolderPlus, ImagePlus, Package, PackageCheck, Plus, RefreshCw, Search, ShieldCheck, Trash2 } from 'lucide-vue-next'
import { goodsApi, goodsAttributeApi, goodsCategoryApi } from '@/api/goods'
import PermissionGate from '@/components/auth/PermissionGate.vue'
import { useAuthStore } from '@/stores/auth'
import type { GoodsAttribute, GoodsCategory, GoodsDetail, GoodsPageItem, GoodsStatus } from '@/types/goods'

interface GoodsForm {
  name: string
  categoryId: string
  description: string
  mainImage: string
  price: number
  originalPrice?: number
  stock: number
  unit: string
  sortOrder: number
  imageUrls: string[]
  attributes: Record<string, string>
}

const authStore = useAuthStore()

const categoryCascaderProps = {
  value: 'id',
  label: 'name',
  children: 'children',
  emitPath: false,
  checkStrictly: true
}

const goodsStatusOptions = [
  { label: '下架', value: 0 },
  { label: '上架', value: 1 },
  { label: '违规下架', value: 2 }
] as const

const loading = ref(false)
const saving = ref(false)
const goodsList = ref<GoodsPageItem[]>([])
const categoryTree = ref<GoodsCategory[]>([])
const attributes = ref<GoodsAttribute[]>([])

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const filters = reactive<{
  name: string
  categoryId: string
  status?: GoodsStatus
  minPrice?: number
  maxPrice?: number
}>({
  name: '',
  categoryId: '',
  status: undefined,
  minPrice: undefined,
  maxPrice: undefined
})

const goodsFormRef = ref<FormInstance>()
const formDrawerVisible = ref(false)
const editingGoodsId = ref('')
const goodsForm = reactive<GoodsForm>(createDefaultGoodsForm())
const formDrawerTitle = computed(() => (editingGoodsId.value ? '编辑商品' : '新增商品'))

const detailDrawerVisible = ref(false)
const detailLoading = ref(false)
const selectedGoods = ref<GoodsDetail | null>(null)

const stockDialogVisible = ref(false)
const adjustingStock = ref(false)
const stockDelta = ref(1)
const stockTarget = ref<GoodsPageItem | null>(null)

const categoryDialogVisible = ref(false)
const savingCategory = ref(false)
const categoryFormRef = ref<FormInstance>()
const categoryForm = reactive({
  parentId: '0',
  name: '',
  icon: '',
  sortOrder: 0
})

const goodsRules: FormRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
  price: [
    {
      validator: (_rule, value, callback) => {
        Number(value) > 0 ? callback() : callback(new Error('售价必须大于 0'))
      },
      trigger: 'blur'
    }
  ],
  stock: [
    {
      validator: (_rule, value, callback) => {
        Number(value) >= 0 ? callback() : callback(new Error('库存不能为负数'))
      },
      trigger: 'blur'
    }
  ]
}

const categoryRules: FormRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

const parentCategoryOptions = computed(() => [
  {
    id: '0',
    name: '一级分类',
    children: categoryTree.value
  }
])

function createDefaultGoodsForm(): GoodsForm {
  return {
    name: '',
    categoryId: '',
    description: '',
    mainImage: '',
    price: 0.01,
    originalPrice: undefined,
    stock: 100,
    unit: '件',
    sortOrder: 0,
    imageUrls: [''],
    attributes: {}
  }
}

function resetGoodsForm() {
  Object.assign(goodsForm, createDefaultGoodsForm())
}

async function loadGoods() {
  loading.value = true
  try {
    const page = await goodsApi.page({
      current: pagination.current,
      size: pagination.size,
      name: filters.name || undefined,
      categoryId: filters.categoryId || undefined,
      status: filters.status,
      minPrice: filters.minPrice,
      maxPrice: filters.maxPrice
    })
    goodsList.value = page.records || []
    pagination.total = Number(page.total || 0)
    pagination.current = Number(page.current || pagination.current)
    pagination.size = Number(page.size || pagination.size)
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  categoryTree.value = await goodsCategoryApi.tree()
}

async function loadAttributes() {
  attributes.value = await goodsAttributeApi.list()
}

function resetFilters() {
  filters.name = ''
  filters.categoryId = ''
  filters.status = undefined
  filters.minPrice = undefined
  filters.maxPrice = undefined
  pagination.current = 1
  void loadGoods()
}

function openCreateDrawer() {
  editingGoodsId.value = ''
  resetGoodsForm()
  formDrawerVisible.value = true
}

async function openEdit(row: GoodsPageItem) {
  editingGoodsId.value = String(row.id)
  resetGoodsForm()
  formDrawerVisible.value = true
  try {
    const detail = await goodsApi.detail(String(row.id))
    populateGoodsForm(detail)
  } catch (error) {
    const message = error instanceof Error ? error.message : '商品详情加载失败'
    ElMessage.error(message)
  }
}

async function openDetail(row: GoodsPageItem) {
  selectedGoods.value = null
  detailDrawerVisible.value = true
  detailLoading.value = true
  try {
    selectedGoods.value = await goodsApi.detail(String(row.id))
  } finally {
    detailLoading.value = false
  }
}

function populateGoodsForm(goods: GoodsDetail) {
  const imageUrls = goods.images?.map((image) => image.imageUrl).filter(Boolean) || []
  Object.assign(goodsForm, {
    name: goods.name,
    categoryId: String(goods.categoryId || ''),
    description: goods.description || '',
    mainImage: goods.mainImage || '',
    price: Number(goods.price || 0.01),
    originalPrice: goods.originalPrice ? Number(goods.originalPrice) : undefined,
    stock: Number(goods.stock || 0),
    unit: goods.unit || '件',
    sortOrder: Number(goods.sortOrder || 0),
    imageUrls: imageUrls.length ? imageUrls : [''],
    attributes: {}
  })
  goods.attributeValues?.forEach((item) => {
    goodsForm.attributes[String(item.attributeId)] = item.attributeValue
  })
}

function buildPayload() {
  const imageUrls = [goodsForm.mainImage, ...goodsForm.imageUrls]
    .map((url) => url.trim())
    .filter((url, index, list) => url && list.indexOf(url) === index)
  return {
    name: goodsForm.name,
    categoryId: goodsForm.categoryId,
    description: goodsForm.description || undefined,
    mainImage: goodsForm.mainImage || imageUrls[0] || undefined,
    price: Number(goodsForm.price || 0),
    originalPrice: goodsForm.originalPrice ? Number(goodsForm.originalPrice) : undefined,
    stock: Number(goodsForm.stock || 0),
    unit: goodsForm.unit || '件',
    sortOrder: Number(goodsForm.sortOrder || 0),
    imageUrls,
    attributeValues: Object.entries(goodsForm.attributes)
      .filter(([, value]) => value)
      .map(([attributeId, attributeValue]) => ({
        attributeId,
        attributeValue
      }))
  }
}

async function submitGoods() {
  await goodsFormRef.value?.validate()
  if (goodsForm.originalPrice && Number(goodsForm.originalPrice) < Number(goodsForm.price)) {
    ElMessage.warning('原价不能低于售价')
    return
  }
  saving.value = true
  try {
    const payload = buildPayload()
    if (editingGoodsId.value) {
      await goodsApi.update(editingGoodsId.value, payload)
      ElMessage.success('商品已更新')
    } else {
      await goodsApi.create(payload)
      ElMessage.success('商品已创建')
      pagination.current = 1
    }
    formDrawerVisible.value = false
    await loadGoods()
  } finally {
    saving.value = false
  }
}

function openStockDialog(row: GoodsPageItem) {
  stockTarget.value = row
  stockDelta.value = 1
  stockDialogVisible.value = true
}

async function submitStock() {
  if (!stockTarget.value?.id) return
  if (stockDelta.value === 0) {
    ElMessage.warning('调整数量不能为 0')
    return
  }
  adjustingStock.value = true
  try {
    await goodsApi.adjustStock({
      goodsId: stockTarget.value.id,
      quantity: stockDelta.value
    })
    ElMessage.success('库存已调整')
    stockDialogVisible.value = false
    await loadGoods()
  } finally {
    adjustingStock.value = false
  }
}

async function changeStatus(row: GoodsPageItem, status: GoodsStatus) {
  const actionText = status === 1 ? '上架' : '下架'
  await ElMessageBox.confirm(`确认${actionText}“${row.name}”？`, `${actionText}商品`, {
    confirmButtonText: `确认${actionText}`,
    cancelButtonText: '取消',
    type: status === 1 ? 'success' : 'warning'
  })
  await goodsApi.updateStatus(String(row.id), status)
  ElMessage.success(`商品已${actionText}`)
  await loadGoods()
}

async function deleteGoods(row: GoodsPageItem) {
  await ElMessageBox.confirm(`确认删除“${row.name}”？`, '删除商品', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await goodsApi.delete(String(row.id))
  ElMessage.success('商品已删除')
  await loadGoods()
}

function openCategoryDialog() {
  Object.assign(categoryForm, {
    parentId: '0',
    name: '',
    icon: '',
    sortOrder: 0
  })
  categoryDialogVisible.value = true
}

async function submitCategory() {
  await categoryFormRef.value?.validate()
  savingCategory.value = true
  try {
    await goodsCategoryApi.create({
      parentId: categoryForm.parentId || '0',
      name: categoryForm.name,
      icon: categoryForm.icon || undefined,
      sortOrder: categoryForm.sortOrder
    })
    ElMessage.success('分类已创建')
    categoryDialogVisible.value = false
    await loadCategories()
  } finally {
    savingCategory.value = false
  }
}

function addImageUrl() {
  goodsForm.imageUrls.push('')
}

function removeImageUrl(index: number) {
  goodsForm.imageUrls.splice(index, 1)
}

function goodsStatusText(status?: GoodsStatus) {
  return goodsStatusOptions.find((item) => item.value === status)?.label || '未知'
}

function goodsStatusTagType(status?: GoodsStatus) {
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  return 'info'
}

function stockTagType(stock?: number) {
  if (!stock || stock <= 0) return 'danger'
  if (stock <= 10) return 'warning'
  return 'success'
}

function money(value?: number) {
  return Number(value || 0).toFixed(2)
}

function categoryNameById(categoryId?: string | number) {
  const id = String(categoryId || '')
  return flattenCategories(categoryTree.value).find((category) => String(category.id) === id)?.name
}

function flattenCategories(tree: GoodsCategory[]): GoodsCategory[] {
  return tree.flatMap((category) => [category, ...flattenCategories(category.children || [])])
}

function attributeOptions(attribute: GoodsAttribute) {
  return (attribute.values || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

function onImageError(event: Event) {
  const image = event.target as HTMLImageElement
  image.style.display = 'none'
}

onMounted(async () => {
  await Promise.all([loadCategories(), loadAttributes()])
  await loadGoods()
})
</script>

<style scoped>
.goods-page :deep(.el-cascader),
.goods-page :deep(.el-select),
.goods-page :deep(.el-input-number) {
  width: 100%;
}

.price-range {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 24px minmax(0, 1fr);
  align-items: center;
  gap: 8px;
}

.price-range span {
  color: var(--color-muted);
  text-align: center;
}

.goods-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.goods-thumb {
  display: grid;
  flex: 0 0 auto;
  width: 58px;
  height: 58px;
  place-items: center;
  overflow: hidden;
  border: 1px solid rgba(0, 167, 199, 0.16);
  border-radius: 8px;
  color: var(--color-muted);
  background:
    linear-gradient(135deg, rgba(231, 251, 255, 0.9), rgba(255, 255, 255, 0.9));
}

.goods-thumb img,
.goods-detail-hero__image img,
.goods-image-grid img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.goods-cell__text {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.goods-cell__text strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-cell__text span,
.price-stack span,
.goods-stat span,
.goods-detail-hero p {
  color: var(--color-muted);
  font-size: 12px;
}

.price-stack {
  display: grid;
  gap: 3px;
}

.price-stack strong {
  color: var(--color-accent);
  font-family: var(--font-family-number);
}

.price-stack span {
  text-decoration: line-through;
}

.goods-stat {
  display: grid;
  gap: 6px;
}

.image-url-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 40px;
  gap: 10px;
}

.image-url-list {
  display: grid;
  gap: 8px;
}

.goods-detail-hero {
  display: grid;
  grid-template-columns: 128px minmax(0, 1fr);
  gap: 16px;
  align-items: center;
  padding: 16px;
  border: 1px solid rgba(0, 167, 199, 0.16);
  border-radius: 12px;
  background:
    radial-gradient(circle at 100% 0, rgba(66, 211, 146, 0.16), transparent 30%),
    linear-gradient(135deg, rgba(247, 254, 255, 0.96), rgba(255, 255, 255, 0.92));
}

.goods-detail-hero__image {
  display: grid;
  width: 128px;
  height: 128px;
  place-items: center;
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  color: var(--color-muted);
  background: #fafafa;
}

.goods-detail-hero h3 {
  margin: 10px 0 8px;
  line-height: 1.4;
}

.goods-image-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.goods-image-grid img {
  aspect-ratio: 1;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: #fafafa;
}

@media (max-width: 900px) {
  .price-range,
  .goods-detail-hero {
    grid-template-columns: 1fr;
  }

  .goods-image-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
