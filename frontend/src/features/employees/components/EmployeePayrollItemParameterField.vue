<script setup lang="ts">
import { computed, ref } from 'vue'
import type { EmployeePayrollItemParameterDefinition } from '../types/employeeApiTypes'
import { formatNumberWithUnit } from '@/shared/utils/UnitFormatUtils'

const props = defineProps<{
  modelValue?: string
  definition: EmployeePayrollItemParameterDefinition
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void
}>()

const textValue = computed({
  get: () => props.modelValue ?? '',
  set: (value: string | number | null) => emit('update:modelValue', value == null ? '' : String(value)),
})

const booleanValue = computed({
  get: () => props.modelValue === 'true',
  set: (value: boolean | null) => emit('update:modelValue', String(Boolean(value))),
})

const focused = ref(false)

const numberUnit = computed(() => {
  const key = props.definition.key.toLowerCase()
  const name = props.definition.displayName

  if (
    /(amount|price|fee|salary|wage)/.test(key)
    || /(金額|料金|単価|日額|月額|給与)/.test(name)
  ) return '円'
  if (/days?/.test(key) || /日数/.test(name)) return '日'
  if (/hours?/.test(key) || /時間/.test(name)) return '時間'
  if (/(people|persons|passengercount)/.test(key) || /人数/.test(name)) return '人'
  if (/count/.test(key) || /回数/.test(name)) return '回'

  return ''
})

const displayedTextValue = computed(() => {
  if (
    props.definition.inputType !== 'NUMBER'
    || focused.value
    || !numberUnit.value
  ) return textValue.value

  return formatNumberWithUnit(textValue.value, numberUnit.value)
})
</script>

<template>
  <v-select
    v-if="definition.inputType === 'SELECT'"
    v-model="textValue"
    :label="definition.displayName"
    :items="definition.options.map((option) => ({ title: option.label, value: option.value }))"
    :required="definition.required"
    density="compact"
    variant="outlined"
  />

  <v-switch
    v-else-if="definition.inputType === 'BOOLEAN'"
    v-model="booleanValue"
    :label="definition.displayName"
    color="primary"
    hide-details
  />

  <v-text-field
    v-else
    :model-value="displayedTextValue"
    :label="definition.displayName"
    :type="
      definition.inputType === 'NUMBER' && focused
        ? 'number'
        : definition.inputType === 'DATE'
          ? 'date'
          : 'text'
    "
    :required="definition.required"
    density="compact"
    variant="outlined"
    @update:model-value="textValue = $event == null ? '' : String($event)"
    @focus="focused = true"
    @blur="focused = false"
  />
</template>
