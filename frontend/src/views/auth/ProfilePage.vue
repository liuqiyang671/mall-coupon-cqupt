<template>
  <PermissionGate>
    <section class="profile-grid">
      <article class="surface-card profile-card">
        <div class="section-heading">
          <div>
            <p class="eyebrow">Profile</p>
            <h3>个人信息</h3>
          </div>
          <el-button :icon="RefreshCw" :loading="loading" @click="loadUserInfo">刷新</el-button>
        </div>

        <el-skeleton v-if="loading && !authStore.userInfo" :rows="5" animated />
        <el-form v-else ref="formRef" :model="form" :rules="rules" label-position="top">
          <div class="readonly-grid">
            <div>
              <span>账号身份</span>
              <strong>{{ authStore.roleLabel }}</strong>
            </div>
            <div>
              <span>用户名</span>
              <strong>{{ authStore.userInfo?.username || authStore.username }}</strong>
            </div>
            <div v-if="authStore.isMerchant">
              <span>店铺编号</span>
              <strong>{{ authStore.userInfo?.shopNumber || '待分配' }}</strong>
            </div>
            <div>
              <span>账号状态</span>
              <strong>{{ accountStatusText }}</strong>
            </div>
          </div>

          <el-form-item label="昵称" prop="nickname">
            <el-input v-model.trim="form.nickname" placeholder="请输入昵称" clearable />
          </el-form-item>
          <el-form-item label="真实姓名" prop="realName">
            <el-input v-model.trim="form.realName" placeholder="请输入真实姓名" clearable />
          </el-form-item>
          <el-form-item label="头像地址" prop="avatarUrl">
            <el-input v-model.trim="form.avatarUrl" placeholder="请输入头像 URL" clearable />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model.trim="form.phone" placeholder="请输入手机号" clearable />
          </el-form-item>
          <el-form-item label="邮箱" prop="mail">
            <el-input v-model.trim="form.mail" placeholder="请输入邮箱" clearable />
          </el-form-item>

          <div class="profile-meta">
            <span>最后登录：{{ authStore.userInfo?.lastLoginTime || '-' }}</span>
            <span>创建时间：{{ authStore.userInfo?.createTime || '-' }}</span>
            <span>更新时间：{{ authStore.userInfo?.updateTime || '-' }}</span>
          </div>

          <div class="form-actions">
            <el-button type="primary" :icon="Save" :loading="saving" @click="saveProfile">保存修改</el-button>
            <el-button :icon="KeyRound" @click="passwordDialogVisible = true">修改密码</el-button>
          </div>
        </el-form>
      </article>

      <article class="surface-card security-card">
        <div class="section-heading">
          <div>
            <p class="eyebrow">Security</p>
            <h3>权限控制</h3>
          </div>
        </div>
        <ul class="security-list">
          <li>
            <ShieldCheck :size="20" />
            <span>当前账号身份为 {{ authStore.roleLabel }}，页面访问由路由守卫保护。</span>
          </li>
          <li>
            <KeyRound :size="20" />
            <span>接口请求自动携带 JWT Token，并由后端校验 Redis 会话。</span>
          </li>
          <li>
            <LogOut :size="20" />
            <span>退出登录或修改密码会清理本地登录态。</span>
          </li>
          <li v-if="authStore.isMerchant || authStore.isPlatform">
            <ShieldCheck :size="20" />
            <span v-if="authStore.isPlatform">优惠券模板按平台券边界管理，不直接修改商家券。</span>
            <span v-else>优惠券模板和推送任务仅可操作当前店铺数据，并按店铺编号隔离。</span>
          </li>
        </ul>
      </article>
    </section>

    <ChangePasswordDialog v-model="passwordDialogVisible" />
  </PermissionGate>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { KeyRound, LogOut, RefreshCw, Save, ShieldCheck } from 'lucide-vue-next'
import ChangePasswordDialog from '@/components/auth/ChangePasswordDialog.vue'
import PermissionGate from '@/components/auth/PermissionGate.vue'
import { useAuthStore } from '@/stores/auth'
import { validateMail, validatePhone } from '@/utils/validators'

const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const passwordDialogVisible = ref(false)

const form = reactive({
  nickname: '',
  realName: '',
  avatarUrl: '',
  phone: '',
  mail: ''
})

const rules: FormRules<typeof form> = {
  phone: [
    {
      validator: (_rule, value, callback) => {
        const message = validatePhone(value)
        message ? callback(new Error(message)) : callback()
      },
      trigger: 'blur'
    }
  ],
  mail: [
    {
      validator: (_rule, value, callback) => {
        const message = validateMail(value)
        message ? callback(new Error(message)) : callback()
      },
      trigger: 'blur'
    }
  ]
}

watch(
  () => authStore.userInfo,
  (userInfo) => {
    form.nickname = userInfo?.nickname || ''
    form.realName = userInfo?.realName || ''
    form.avatarUrl = userInfo?.avatarUrl || ''
    form.phone = userInfo?.phone || ''
    form.mail = userInfo?.mail || ''
  },
  { immediate: true }
)

const accountStatusText = computed(() => {
  if (!authStore.userInfo) return '-'
  if (authStore.userInfo.status === 1) return '已禁用'
  if (authStore.userInfo.activationStatus === 0) return '未激活'
  return '正常'
})

async function loadUserInfo() {
  loading.value = true
  try {
    await authStore.fetchUserInfo()
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  await formRef.value?.validate()
  saving.value = true
  try {
    await authStore.updateProfile({
      nickname: form.nickname || undefined,
      realName: form.realName || undefined,
      avatarUrl: form.avatarUrl || undefined,
      phone: form.phone || undefined,
      mail: form.mail || undefined
    })
    ElMessage.success('个人信息已更新')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  if (!authStore.userInfo) {
    void loadUserInfo()
  }
})
</script>
