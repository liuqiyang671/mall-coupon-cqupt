<template>
  <AuthLayout>
    <div class="form-card">
      <div class="form-card__header">
        <p class="eyebrow">账号安全</p>
        <h2>找回密码</h2>
        <p>后端暂未提供验证码和重置密码接口，当前页面提供完整前端流程与受控降级提示。</p>
      </div>

      <el-steps :active="activeStep" finish-status="success" align-center class="recover-steps">
        <el-step title="确认账号" />
        <el-step title="验证身份" />
        <el-step title="重置指引" />
      </el-steps>

      <section v-if="activeStep === 0" class="recover-step-panel">
        <el-form ref="accountFormRef" :model="accountForm" :rules="accountRules" label-position="top">
          <el-form-item label="账号身份" prop="roleType">
            <el-radio-group v-model="accountForm.roleType" class="role-selector">
              <el-radio-button v-for="role in roleOptions" :key="role.value" :label="role.value">
                {{ role.label }}
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="用户名" prop="username">
            <el-input v-model.trim="accountForm.username" placeholder="请输入需要找回的用户名" />
          </el-form-item>
          <el-button class="form-submit" type="primary" :icon="ShieldCheck" @click="confirmAccount">下一步</el-button>
        </el-form>
      </section>

      <section v-else-if="activeStep === 1" class="recover-step-panel">
        <el-form ref="verifyFormRef" :model="verifyForm" :rules="verifyRules" label-position="top">
          <el-form-item label="安全联系方式" prop="contact">
            <el-input v-model.trim="verifyForm.contact" placeholder="请输入绑定手机号或邮箱" />
          </el-form-item>
          <el-alert title="当前后端未开放验证码发送接口，提交后将生成找回指引。" type="warning" show-icon :closable="false" />
          <div class="form-actions">
            <el-button @click="activeStep = 0">上一步</el-button>
            <el-button type="primary" :icon="KeyRound" @click="confirmContact">生成指引</el-button>
          </div>
        </el-form>
      </section>

      <section v-else class="recover-result">
        <el-result icon="warning" title="需要后端接口支持" sub-title="请联系系统管理员重置密码，或在后端补充验证码与重置密码接口后接入当前流程。">
          <template #extra>
            <el-button type="primary" @click="router.replace({ name: 'login' })">返回登录</el-button>
          </template>
        </el-result>
      </section>
    </div>
  </AuthLayout>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { type FormInstance, type FormRules } from 'element-plus'
import { KeyRound, ShieldCheck } from 'lucide-vue-next'
import AuthLayout from '@/layouts/AuthLayout.vue'
import { USER_ROLE_OPTIONS, type UserRoleType } from '@/types/auth'
import { validateMail, validatePhone, validateUsername } from '@/utils/validators'

const router = useRouter()
const activeStep = ref(0)
const accountFormRef = ref<FormInstance>()
const verifyFormRef = ref<FormInstance>()
const roleOptions = USER_ROLE_OPTIONS

const accountForm = reactive({ roleType: 1 as UserRoleType, username: '' })
const verifyForm = reactive({ contact: '' })

const accountRules: FormRules<typeof accountForm> = {
  roleType: [{ required: true, message: '请选择账号身份', trigger: 'change' }],
  username: [
    {
      validator: (_rule, value, callback) => {
        const message = validateUsername(value)
        message ? callback(new Error(message)) : callback()
      },
      trigger: 'blur'
    }
  ]
}

const verifyRules: FormRules<typeof verifyForm> = {
  contact: [
    {
      validator: (_rule, value, callback) => {
        if (!value) {
          callback(new Error('请输入手机号或邮箱'))
          return
        }
        const valid = !validatePhone(value) || !validateMail(value)
        valid ? callback() : callback(new Error('请输入正确的手机号或邮箱'))
      },
      trigger: 'blur'
    }
  ]
}

async function confirmAccount() {
  await accountFormRef.value?.validate()
  activeStep.value = 1
}

async function confirmContact() {
  await verifyFormRef.value?.validate()
  activeStep.value = 2
}
</script>
