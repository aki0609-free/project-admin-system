<script setup lang="ts" generic="T extends Record<string, any>">
import { FormFieldDef, NestedFormContextKey } from '@/shared/components/form/base/types/types';
import { inject, computed } from 'vue'
import NestedEditableFormCell from '../base/NestedEditableFormCell.vue';

const props = defineProps<{
  name: Extract<keyof T, string>
  fields: FormFieldDef<any>[]
}>()

const form = inject<any>(NestedFormContextKey)
if (!form) {
  throw new Error('NestedRepeatableForm must be used inside FormLayout')
}

const items = computed(() => {
  return form.model.value[props.name] ?? []
})

const addItem = () => {
  if (!form.model.value[props.name]) {
    form.model.value[props.name] = []
  }
  form.model.value[props.name].push({})
}

const removeItem = (index: number) => {
  form.model.value[props.name].splice(index, 1)
  form.validateAll()
}
</script>

<template>
  <div>
    <div
      v-for="(item, index) in items"
      :key="index"
      class="nested-item"
    >
      <NestedEditableFormCell
        v-for="field in fields"
        :key="String(field.key)"
        :field="field"
        :model="item"
        :path="[name, index]"
      />

      <v-btn
        color="error"
        variant="text"
        @click="removeItem(Number(index))"
      >
        Remove
      </v-btn>
    </div>

    <v-btn
      color="primary"
      variant="outlined"
      @click="addItem"
    >
      Add
    </v-btn>
  </div>
</template>