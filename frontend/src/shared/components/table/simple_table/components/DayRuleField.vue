<!-- eslint-disable @typescript-eslint/no-explicit-any -->
<script setup lang="ts">
import type { DayRule } from '@/shared/types/dayRuleTypes'

const props = defineProps<{
  modelValue: DayRule | null
  label?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: DayRule | null): void
}>()

const monthOffsetItems = [
  { title: '当月', value: 0 },
  { title: '翌月', value: 1 },
  { title: '翌々月', value: 2 },
]

const typeItems = [
  { title: '日付指定', value: 'DAY_OF_MONTH' },
  { title: '末日', value: 'END_OF_MONTH' },
]

function updateMonthOffset(monthOffset: number) {
  emit('update:modelValue', {
    type: props.modelValue?.type ?? 'DAY_OF_MONTH',
    value: props.modelValue?.type === 'END_OF_MONTH'
      ? null
      : props.modelValue?.value ?? 1,
    monthOffset,
  })
}

function updateType(type: string) {
  emit('update:modelValue', {
    type: type as any,
    value: type === 'END_OF_MONTH'
      ? null
      : props.modelValue?.value ?? 1,
    monthOffset: props.modelValue?.monthOffset ?? 0,
  })
}

function updateValue(value: number | string) {
  emit('update:modelValue', {
    type: props.modelValue?.type ?? 'DAY_OF_MONTH',
    value: Number(value),
    monthOffset: props.modelValue?.monthOffset ?? 0,
  })
}
</script>

<template>
  <div class="d-flex ga-2 align-center">
    <v-select
      :model-value="modelValue?.monthOffset ?? 0"
      :items="monthOffsetItems"
      item-title="title"
      item-value="value"
      density="compact"
      hide-details
      style="max-width: 120px"
      @update:model-value="updateMonthOffset"
    />

    <v-select
      :model-value="modelValue?.type"
      :items="typeItems"
      item-title="title"
      item-value="value"
      density="compact"
      hide-details
      style="max-width: 180px"
      @update:model-value="updateType"
    />

    <v-text-field
      v-if="modelValue?.type === 'DAY_OF_MONTH'"
      :model-value="modelValue?.value"
      type="number"
      density="compact"
      hide-details
      suffix="日"
      style="max-width: 120px"
      @update:model-value="updateValue"
    />

    <div
      v-else-if="modelValue?.type === 'END_OF_MONTH'"
      class="text-caption text-medium-emphasis"
    >
      月末
    </div>
  </div>
</template>