<script setup lang="ts" generic="T extends Record<string, any>">
import { NestedFormContextKey } from '@/shared/components/form/base/types/types';
import { NestedObjectFormFieldDef } from '@/shared/components/form/nested_object_form/types/types';
import { computed, inject } from 'vue'
import NestedRepeatableForm from '../nested_repeatable_form/NestedRepeatableForm.vue';
import NestedEditableFormCell from '../base/NestedEditableFormCell.vue';

const props = defineProps<{
  fields: NestedObjectFormFieldDef<T>[]
  path?: (string | number)[]
}>()

const form = inject<any>(NestedFormContextKey)
if (!form) {
  throw new Error('NestedRepeatableForm must be used inside FormLayout')
}

const visibleFields = computed(() =>
  props.fields.filter(f => {
    if (!f.showIf) return true
    return f.showIf(form.model)
  })
)
</script>

<template>
  <div>
    <template v-for="field in visibleFields" :key="String(field.key)">
      <template v-if="field.type === 'object' && field.fields">
        <NestedObjectForm
          :model="form.model[field.key]"
          :fields="field.fields"
          :path="[...(path ?? []), String(field.key)]"
        />
      </template>

      <template v-else-if="field.type === 'array' && field.itemFields">
        <NestedRepeatableForm
          :name="String(field.key)"
          :fields="field.itemFields"
          :path="[...(path ?? [])]"
          :model="form.model"
        />
      </template>

      <template v-else>
        <NestedEditableFormCell
          :field="field"
          :model="form.model"
          :path="path!"
        />
      </template>
    </template>
  </div>
</template>