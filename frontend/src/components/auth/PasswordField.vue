<template>
  <el-input
    :model-value="modelValue"
    :type="visible ? 'text' : 'password'"
    :placeholder="placeholder"
    autocomplete="current-password"
    clearable
    @update:model-value="emit('update:modelValue', String($event))"
  >
    <template #suffix>
      <button class="input-icon-button" type="button" :aria-label="visible ? '隐藏密码' : '显示密码'" @click="visible = !visible">
        <EyeOff v-if="visible" :size="18" />
        <Eye v-else :size="18" />
      </button>
    </template>
  </el-input>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Eye, EyeOff } from 'lucide-vue-next'

withDefaults(
  defineProps<{
    modelValue: string
    placeholder?: string
  }>(),
  {
    placeholder: '请输入密码'
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const visible = ref(false)
</script>
