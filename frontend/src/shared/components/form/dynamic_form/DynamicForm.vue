<script setup lang="ts" generic="T extends Record<string, any>">
import { FormContextKey } from '@/shared/components/form/base/types/types'
import { DynamicFormFieldDef } from '@/shared/components/form/dynamic_form/types/types'
import { computed, inject } from 'vue'
import EditableFormCell from '../base/EditableFormCell.vue';

const props = defineProps<{
  fields: DynamicFormFieldDef<T>[]
}>()

const form = inject(FormContextKey)
if (!form) {
  throw new Error('DynamicForm must be used inside FormLayout')
}

const visibleFields = computed(() =>
  props.fields.filter((field) => {
    if (!field.showIf) return true
    return field.showIf?.(form.model.value as T)
  }),
)
</script>

<template>
  <div>
    <EditableFormCell v-for="field in visibleFields" :key="String(field.key)" :field="field" />
  </div>
</template>
