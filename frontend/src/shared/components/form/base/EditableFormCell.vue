<!-- eslint-disable @typescript-eslint/no-explicit-any -->
<!-- eslint-disable @typescript-eslint/consistent-type-imports -->
<script setup lang="ts" generic="T extends Record<string, any>">
import {
  FormContext,
  FormContextKey,
  FormFieldDef,
} from '@/shared/components/form/base/types/types'
import { componentMap } from '@/shared/components/form/base/utils/componentMap'
import { computed, inject, nextTick, ref } from 'vue'

const props = defineProps<{
  field: FormFieldDef<T>
}>()

const formContext = inject<FormContext>(FormContextKey)
if (!formContext) {
  throw new Error('EditableFormCell must be used inside a FormProvider')
}

const model = formContext.model as any
const isFocused = ref(false)
const draftValue = ref<unknown>('')

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

  if (props.field.parser && isFocused.value) {
    return draftValue.value
  }

  return rawValue.value
})

const inputType = computed(() => {
  // formatter表示中に number を付けると "10,000円" が表示できず消える
  if (shouldUseFormattedDisplay.value) return undefined
  if (!isEditable.value) return undefined

  if (props.field.type === 'password') return 'password'
  if (props.field.type === 'number') return 'number'
  if (props.field.type === 'month') return 'month'
  return undefined
})

const updateValue = (val: unknown) => {
  if (!isEditable.value) return

  if (props.field.parser) {
    draftValue.value = val
    return
  }

  // formatter表示中に入ってくるのは避けて、編集中の生値だけ保存
  if (props.field.type === 'number') {
    if (val == null || val === '') {
      model.value[props.field.key] = null
      props.field.onUpdate?.(null, model.value)
      return
    }

    const numericValue = typeof val === 'number' ? val : Number(val)
    model.value[props.field.key] = Number.isFinite(numericValue)
      ? numericValue
      : val
    props.field.onUpdate?.(
      model.value[props.field.key],
      model.value,
    )
    return
  }

  model.value[props.field.key] = val
  props.field.onUpdate?.(val, model.value)

  // 日付ピッカーは入力欄の blur 後に値を反映するため、
  // blur 時の古い必須エラーを新しい値で再評価する。
  if (props.field.type === 'date') {
    void nextTick(() =>
      formContext.validateField(
        String(props.field.key),
      ),
    )
  }
}

const handleFocus = () => {
  if (!isEditable.value) return
  draftValue.value = props.field.parser
    ? displayValue.value
    : rawValue.value
  isFocused.value = true
}

const handleBlur = () => {
  if (props.field.parser) {
    model.value[props.field.key] = props.field.parser(
      draftValue.value,
      model.value,
    )
    props.field.onUpdate?.(
      model.value[props.field.key],
      model.value,
    )
  }
  isFocused.value = false
  void nextTick(() =>
    formContext.validateField(String(props.field.key)),
  )
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
