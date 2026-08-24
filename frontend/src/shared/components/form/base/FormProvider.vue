<script setup lang="ts" generic="Schema extends ZodObject">
import { FormContextKey } from '@/shared/components/form/base/types/types'
import { computed, provide, ref } from 'vue'
import type { ZodObject, ZodType, z } from 'zod'

// Type Definition
type T = z.infer<Schema>

// Props & Emit
const props = defineProps<{
  modelValue: T
  schema: Schema
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: T): void
}>()

// Model
const model = computed<T>({
  get: () => props.modelValue,
  set: val => emit('update:modelValue', val),
})

// Error
const errors = ref<Partial<Record<string, string[]>>>({})

// Entire Validation
function validate(): boolean {
  const result = props.schema.safeParse(model.value)

  if (!result.success) {
    const formatted: Record<string, string[]> = {}

    result.error.issues.forEach(issue => {
      const key = issue.path[0] as string
      if (!formatted[key]) formatted[key] = []
      formatted[key].push(issue.message)
    })

    errors.value = formatted
    return false
  }

  errors.value = {}
  return true
}

// Single Validation
function validateField(key: keyof T) {
  const errorKey = String(key)
  const fieldSchema = (props.schema.shape as Record<string, ZodType>)[errorKey]

  // A form schema may intentionally validate only a subset of the displayed
  // fields, so fields without an explicit schema are treated as valid.
  if (!fieldSchema) {
    clearFieldError(errorKey)
    return true
  }

  const result = fieldSchema.safeParse(model.value[key])

  if (!result.success) {
    errors.value[errorKey] = result.error.issues.map(i => i.message)
    return false
  }

  clearFieldError(errorKey)
  return true
}

function clearFieldError(key: string) {
  const nextErrors = { ...errors.value }
  Reflect.deleteProperty(nextErrors, key)
  errors.value = nextErrors
}

defineExpose({
  validateAll: validate
})

// Provide
provide(FormContextKey, {
  model,
  errors,
  validateField,
  validateAll: validate
})
</script>

<template>
  <slot />
</template>
