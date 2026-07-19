<script setup lang="ts">
import { computed, ref } from 'vue'
import { formatYearMonthDay } from '@/shared/utils/DateUtils'

const props = defineProps<{
  modelValue?: string | null
  label?: string
  density?: 'default' | 'comfortable' | 'compact'
  disabled?: boolean
  readonly?: boolean
  clearable?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const menu = ref(false)

const internalValue = computed({
  get: () => props.modelValue ?? '',
  set: (value: string) => {
    emit('update:modelValue', normalizeDateValue(value))
  },
})

const displayValue = computed(() =>
  formatYearMonthDay(props.modelValue ?? ''),
)

function handleDateUpdate(value: unknown) {
  const normalized = normalizeDateValue(value)
  emit('update:modelValue', normalized)
  menu.value = false
}

function clearValue() {
  emit('update:modelValue', '')
}

function normalizeDateValue(value: unknown): string {
  if (!value) return ''

  if (Array.isArray(value)) {
    return value[0] ? String(value[0]) : ''
  }

  if (value instanceof Date) {
    return formatDateToIso(value)
  }

  return String(value)
}

function formatDateToIso(date: Date): string {
  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}
</script>

<template>
  <v-menu
    v-model="menu"
    :close-on-content-click="false"
    transition="scale-transition"
    offset-y
    min-width="auto"
  >
    <template #activator="{ props: menuProps }">
      <v-text-field
        v-bind="menuProps"
        :model-value="displayValue"
        :label="label"
        :density="density ?? 'comfortable'"
        :disabled="disabled"
        :readonly="true"
        :clearable="clearable"
        @click:clear="clearValue"
      />
    </template>

    <v-locale-provider locale="ja">
      <v-date-picker
        :model-value="internalValue"
        header="日付を選択"
        @update:model-value="handleDateUpdate"
      />
    </v-locale-provider>
  </v-menu>
</template>