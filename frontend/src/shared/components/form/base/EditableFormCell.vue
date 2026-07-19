<!-- eslint-disable @typescript-eslint/no-explicit-any -->
<!-- eslint-disable @typescript-eslint/consistent-type-imports -->
<script setup lang="ts" generic="T extends Record<string, any>">
import {
  FormContext,
  FormContextKey,
  FormFieldDef,
} from '@/shared/components/form/base/types/types'
import { componentMap } from '@/shared/components/form/base/utils/componentMap'
import { computed, inject, ref } from 'vue'

const props = defineProps<{
  field: FormFieldDef<T>
}>()

const formContext = inject<FormContext>(FormContextKey)
if (!formContext) {
  throw new Error('EditableFormCell must be used inside a FormProvider')
}

const model = formContext.model as any
const isFocused = ref(false)

const rawValue = computed(() => model.value[props.field.key])

/*
const modelValue = computed({
  get: () => rawValue.value,
  set: (val) => {
    model.value[props.field.key] = val
  },
})
*/

const component = computed(() => {
  return componentMap[props.field.type] ?? componentMap.text
})

const errors = computed(() => formContext.errors.value[props.field.key as string] ?? [])

const isEditable = computed(() => props.field.editable !== false)

const isReadonly = computed(() => {
  if (!isEditable.value) return true
  return false
})

const isDisabled = computed(() => {
  if (isEditable.value) return false
  return ['select', 'checkbox', 'selectboxWithChips'].includes(props.field.type)
})

const displayValue = computed(() => {
  const value = rawValue.value
  return props.field.formatter?.(value, model.value) ?? value ?? ''
})

const shouldUseFormattedDisplay = computed(() => {
  return !!props.field.formatter && !isFocused.value
})

const componentModelValue = computed(() => {
  if (!isEditable.value) {
    return displayValue.value
  }

  if (shouldUseFormattedDisplay.value) {
    return displayValue.value
  }

  return rawValue.value
})

const inputType = computed(() => {
  // formatter表示中に number を付けると "10,000円" が表示できず消える
  if (shouldUseFormattedDisplay.value) return undefined
  if (!isEditable.value) return undefined

  if (props.field.type === 'password') return 'password'
  if (props.field.type === 'number') return 'number'
  return undefined
})

const updateValue = (val: unknown) => {
  if (!isEditable.value) return

  // formatter表示中に入ってくるのは避けて、編集中の生値だけ保存
  model.value[props.field.key] = val
}

const handleFocus = () => {
  if (!isEditable.value) return
  isFocused.value = true
}

const handleBlur = () => {
  isFocused.value = false
  formContext.validateField(String(props.field.key))
}
</script>

<template>
  <component
    :is="component"
    :model-value="componentModelValue"
    :label="field.label"
    :items="field.options"
    item-title="title"
    item-value="value"
    :type="inputType"
    :error="!!errors.length"
    :error-messages="errors"
    :readonly="isReadonly"
    :disabled="isDisabled"
    :rows="field.rows"
    :auto-grow="field.autoGrow"
    variant="outlined"
    density="comfortable"
    hide-details="auto"
    @update:model-value="updateValue"
    @focus="handleFocus"
    @blur="handleBlur"
  />
</template>
