<script setup lang="ts" generic="T extends Record<string, any>">
import { computed, inject } from 'vue'
import { componentMap } from '@/shared/components/form/base/utils/componentMap'
import { NestedFormContextKey, type FormFieldDef } from '@/shared/components/form/base/types/types'

const props = defineProps<{
  field: FormFieldDef<T>
  model: T
  path?: (string | number)[]
}>()

const path = computed(() => props.path ?? [])

const form = inject<any>(NestedFormContextKey)
if (!form) {
  throw new Error('NestedEditableFormCell must be used inside FormLayout')
}

/**
 * v-model binding
 */
const modelValue = computed({
  get: () => props.model[props.field.key],
  set: (val) => {
    props.model[props.field.key] = val
  }
})

/**
 * component resolve
 */
const component = computed(() => {
  return componentMap[props.field.type] ?? componentMap.text
})

/**
 * Zod v4 nested error resolve
 */
const errors = computed(() => {
  const issues = form.zodError?.value?.issues ?? []
  const fullPath = [...path.value, props.field.key]

  return issues
    .filter((issue: any) => {
      if (issue.path.length !== fullPath.length) return false
      return issue.path.every(
        (seg: any, i: number) => seg === fullPath[i]
      )
    })
    .map((issue: any) => issue.message)
})
</script>

<template>
  <component
    :is="component"
    v-model="modelValue"
    :label="field.label"
    :items="field.options"
    :error="!!errors.length"
    :error-messages="errors"
    @blur="form.validateField([...path, field.key])"
  />
</template>