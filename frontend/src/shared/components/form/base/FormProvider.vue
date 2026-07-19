<script setup lang="ts" generic="Schema extends ZodObject<any>">
import { FormContextKey } from '@/shared/components/form/base/types/types';
import { computed, provide, ref } from 'vue';
import z, { ZodObject } from 'zod';

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
  const fieldSchema = props.schema.pick({[key]: true} as any)

  const fieldData = { [key]: model.value[key] }
  const result = fieldSchema.safeParse(fieldData)

  if (!result.success) {
    errors.value[key as string] = result.error.issues.map(i => i.message)
    return false
  }

  delete errors.value[key as string]
  return true
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