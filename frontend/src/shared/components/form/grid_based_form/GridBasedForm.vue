<script setup lang="ts" generic="T extends Record<string, any>">
import { computed } from 'vue'
import { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import EditableFormCell from '../base/EditableFormCell.vue'

const props = defineProps<{
  modelValue: T
  fields: GridFormFieldDef<T>[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: T): void
}>()

const model = computed({
  get: () => props.modelValue,
  set: val => emit('update:modelValue', val)
})
</script>

<template>
  <div class="grid-form">
    <template v-for="field in fields" :key="String(field.key)">
      <div
        class="grid-cell"
        :style="{
          gridColumn: field.gridColumn ?? 'auto',
          minWidth: field.width ? field.width + 'px' : 'auto'
        }"
      >
        <EditableFormCell :field="field" />
      </div>
    </template>
  </div>
</template>

<style scoped>
.grid-form {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.grid-cell {
  display: flex;
  flex-direction: column;
  align-items: stretch;
}
</style>