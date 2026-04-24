<template>
  <AuthLayout>
    <div class="form-card">
      <div class="form-card__header">
        <p class="eyebrow">{{ selectedRole.label }}注册</p>
        <h2>创建{{ selectedRole.label }}账号</h2>
        <p>{{ selectedRole.registerHint }}</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="账号身份" prop="roleType">
          <el-radio-group v-model="form.roleType" class="role-selector">
            <el-radio-button v-for="role in roleOptions" :key="role.value" :label="role.value">
              {{ role.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="form.username" placeholder="3-32 位字母、数字或下划线" clearable />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model.trim="form.nickname" placeholder="用于页面展示，可选" clearable />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model.trim="form.realName" placeholder="用于账号实名备注，可选" clearable />
        </el-form-item>
        <el-form-item v-if="form.roleType === 1" label="店铺编号" prop="shopNumber">
          <el-input v-model.trim="form.shopNumber" placeholder="可选，不填由系统自动生成" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <PasswordField v-model="form.password" placeholder="6-32 位密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <PasswordField v-model="form.confirmPassword" placeholder="请再次输入密码" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model.trim="form.phone" placeholder="可选，用于账号安全联系" clearable />
        </el-form-item>
        <el-form-item label="邮箱" prop="mail">
          <el-input v-model.trim="form.mail" placeholder="可选，用于账号安全联系" clearable />
        </el-form-item>
        <el-button class="form-submit" type="primary" :icon="UserPlus" :loading="submitting" @click="submit">注册</el-button>
      </el-form>

      <p class="form-switch">已有账号？<RouterLink to="/login">去登录</RouterLink></p>
    </div>
  </AuthLayout>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { UserPlus } from 'lucide-vue-next'
import AuthLayout from '@/layouts/AuthLayout.vue'
import PasswordField from '@/components/auth/PasswordField.vue'
import { useAuthStore } from '@/stores/auth'
import { USER_ROLE_OPTIONS, type UserRoleType } from '@/types/auth'
import { validateMail, validatePassword, validatePhone, validateShopNumber, validateUsername } from '@/utils/validators'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const roleOptions = USER_ROLE_OPTIONS

const form = reactive({
  roleType: 1 as UserRoleType,
  username: '',
  nickname: '',
  realName: '',
  shopNumber: '',
  password: '',
  confirmPassword: '',
  phone: '',
  mail: ''
})

const selectedRole = computed(() => roleOptions.find((role) => role.value === form.roleType) || roleOptions[0])

const rules: FormRules<typeof form> = {
  roleType: [{ required: true, message: '请选择账号身份', trigger: 'change' }],
  username: [
    {
      validator: (_rule, value, callback) => {
        const message = validateUsername(value)
        message ? callback(new Error(message)) : callback()
      },
      trigger: 'blur'
    }
  ],
  password: [
    {
      validator: (_rule, value, callback) => {
        const message = validatePassword(value)
        message ? callback(new Error(message)) : callback()
      },
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    {
      validator: (_rule, value, callback) => {
        if (!value) {
          callback(new Error('请再次输入密码'))
          return
        }
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
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
  ],
  shopNumber: [
    {
      validator: (_rule, value, callback) => {
        const message = validateShopNumber(value)
        message ? callback(new Error(message)) : callback()
      },
      trigger: 'blur'
    }
  ]
}

watch(
  () => form.roleType,
  (roleType) => {
    if (roleType !== 1) {
      form.shopNumber = ''
    }
  }
)

async function submit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    await authStore.register({
      roleType: form.roleType,
      username: form.username,
      password: form.password,
      nickname: form.nickname || undefined,
      realName: form.realName || undefined,
      shopNumber: form.roleType === 1 ? form.shopNumber || undefined : undefined,
      phone: form.phone || undefined,
      mail: form.mail || undefined
    })
    ElMessage.success('注册成功，请登录')
    await router.replace({ name: 'login' })
  } finally {
    submitting.value = false
  }
}
</script>
