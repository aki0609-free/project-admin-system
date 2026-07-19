<script setup lang="ts" generic="Schema extends ZodObject<any>">
import { FormContextKey, NestedFormContext, NestedFormContextKey } from '@/shared/components/form/base/types/types'
import { computed, provide, ref } from 'vue'
import z, { ZodObject } from 'zod'

type T = z.infer<Schema>

const props = defineProps<{
  modelValue: T
  schema: Schema
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: T): void
}>()

// v-model bridge
const model = computed<T>({
  get: () => props.modelValue,
  set: val => emit('update:modelValue', val),
})

// フラット互換エラー
const errors = ref<Partial<Record<string, string[]>>>({})

// Nested用
const zodError = ref<z.ZodError<T> | null>(null)

/**
 * 全体バリデーション
 */
function validate(): boolean {
  const result = props.schema.safeParse(model.value)

  if (!result.success) {
    zodError.value = result.error

    const formatted: Record<string, string[]> = {}

    result.error.issues.forEach(issue => {
      const flatKey = issue.path.join('.') // ★ ここ変更
      if (!formatted[flatKey]) formatted[flatKey] = []
      formatted[flatKey].push(issue.message)
    })

    errors.value = formatted
    return false
  }

  zodError.value = null
  errors.value = {}
  return true
}

/**
 * フィールド単位バリデーション
 */
function validateField(key: string | (string | number)[]) {
  // path指定なら全体validateにフォールバック
  if (Array.isArray(key)) {
    return validate()
  }

  // フラット従来動作
  const fieldSchema = props.schema.pick({ [key]: true } as any)
  const fieldData = { [key]: model.value[key as keyof T] }

  const result = fieldSchema.safeParse(fieldData)

  if (!result.success) {
    errors.value[key] = result.error.issues.map(i => i.message)
    return false
  }

  delete errors.value[key]
  return true
}

provide<NestedFormContext<T>>(NestedFormContextKey, {
  model,
  errors,
  zodError,
  validateField,
  validateAll: validate,
})
</script>

<template>
  <slot />
</template>