<script setup lang="ts" generic="T extends Record<string, any>">
import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import EditableFormCell from '../base/EditableFormCell.vue'

defineProps<{
  modelValue: T
  fields: GridFormFieldDef<T>[]
}>()
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
  align-items: start;
  gap: 16px;
}

.grid-cell {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  align-self: start;
  min-width: 0;
  width: 100%;
}
</style>
