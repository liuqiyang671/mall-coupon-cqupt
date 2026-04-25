<template>
  <AuthLayout>
    <div class="form-card">
      <div class="form-card__header">
        <p class="eyebrow">{{ selectedRole.label }}登录</p>
        <h2>登录邮惠券商城</h2>
        <p>{{ selectedRole.loginHint }}</p>
      </div>

      <el-alert v-if="errorMessage" :title="errorMessage" type="error" show-icon :closable="false" class="form-alert" />

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
        <el-form-item label="登录身份" prop="roleType">
          <el-radio-group v-model="form.roleType" class="role-selector">
            <el-radio-button v-for="role in roleOptions" :key="role.value" :label="role.value">
              {{ role.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="form.username" placeholder="请输入用户名" clearable autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <PasswordField v-model="form.password" />
        </el-form-item>
        <div class="form-row-between">
          <el-checkbox v-model="form.remember">记住登录状态</el-checkbox>
          <RouterLink to="/forgot-password">忘记密码？</RouterLink>
        </div>
        <el-button class="form-submit" type="primary" :icon="LogIn" :loading="submitting" @click="submit">登录</el-button>
      </el-form>

      <p class="form-switch">还没有账号？<RouterLink to="/register">立即注册</RouterLink></p>
    </div>
  </AuthLayout>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { LogIn } from 'lucide-vue-next'
import AuthLayout from '@/layouts/AuthLayout.vue'
import PasswordField from '@/components/auth/PasswordField.vue'
import { useAuthStore } from '@/stores/auth'
import { USER_ROLE_OPTIONS, type UserRoleType } from '@/types/auth'
import { validatePassword, validateUsername } from '@/utils/validators'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const errorMessage = ref('')
const roleOptions = USER_ROLE_OPTIONS

const form = reactive({
  roleType: 1 as UserRoleType,
  username: '',
  password: '',
  remember: true
})

const selectedRole = computed(() => roleOptions.find((role) => role.value === form.roleType) || roleOptions[0])

const rules: FormRules<typeof form> = {
  roleType: [{ required: true, message: '请选择登录身份', trigger: 'change' }],
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
  ]
}

async function submit() {
  errorMessage.value = ''
  await formRef.value?.validate()
  submitting.value = true
  try {
    await authStore.login(form)
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : defaultHomePath()
    await router.replace(redirect)
  } catch (error) {
    form.password = ''
    errorMessage.value = error instanceof Error ? error.message : '登录失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}

function defaultHomePath() {
  return authStore.isCustomer ? '/user/products' : '/merchant'
}
</script>
