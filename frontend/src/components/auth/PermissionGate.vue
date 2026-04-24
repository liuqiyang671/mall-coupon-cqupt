<template>
  <slot v-if="allowed" />
  <slot v-else name="fallback">
    <el-empty description="你没有权限访问当前内容" />
  </slot>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import type { UserRoleType } from '@/types/auth'

const props = withDefaults(
  defineProps<{
    requireAuth?: boolean
    roles?: UserRoleType | UserRoleType[]
  }>(),
  {
    requireAuth: true
  }
)

const authStore = useAuthStore()
const allowed = computed(() => {
  if (props.requireAuth && !authStore.isAuthenticated) {
    return false
  }
  if (props.roles === undefined) {
    return true
  }
  const roles = Array.isArray(props.roles) ? props.roles : [props.roles]
  return authStore.roleType !== null && roles.includes(authStore.roleType)
})
</script>
