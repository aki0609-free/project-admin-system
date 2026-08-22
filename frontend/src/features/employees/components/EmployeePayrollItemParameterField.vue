<script setup lang="ts">
import { computed } from 'vue'
import type { EmployeePayrollItemParameterDefinition } from '../types/employeeApiTypes'

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
    v-model="textValue"
    :label="definition.displayName"
    :type="
      definition.inputType === 'NUMBER'
        ? 'number'
        : definition.inputType === 'DATE'
          ? 'date'
          : 'text'
    "
    :required="definition.required"
    density="compact"
    variant="outlined"
  />
</template>
