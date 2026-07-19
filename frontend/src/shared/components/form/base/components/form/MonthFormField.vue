<script setup lang="ts">
import { computed } from 'vue'

defineProps<{
  modelValue?: string | null
  label?: string
  clearable?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const items = computed(() => {
  const currentYear = new Date().getFullYear()

  const result: {
    title: string
    value: string
  }[] = []

  for (let year = currentYear - 3; year <= currentYear + 3; year++) {
    for (let month = 1; month <= 12; month++) {
      result.push({
        title: `${year}年${month}月`,
        value: `${year}-${String(month).padStart(2, '0')}`,
      })
    }
  }

  return result.reverse()
})
</script>

<template>
  <v-select
    :model-value="modelValue"
    :items="items"
    :label="label"
    variant="outlined"
    density="compact"
    hide-details
    clearable
    @update:model-value="emit('update:modelValue', $event ?? '')"
  />
</template>