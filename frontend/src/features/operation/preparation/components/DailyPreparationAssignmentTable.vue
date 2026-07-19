<script setup lang="ts">
import { toRef } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import { type DailyPreparationAssignmentTableRow, useDailyPreparationAssignmentTableConfig } from '../composables/useDailyPreparationAssignmentTableConfig';


const props = defineProps<{
  items: DailyPreparationAssignmentTableRow[]
}>()

const emit = defineEmits<{
  (
    e: 'update:items',
    payload: {
      id: number
      field: keyof DailyPreparationAssignmentTableRow
      value: unknown
    }
  ): void
}>()

const {
  rows,
  columns,
  filterRules,
} = useDailyPreparationAssignmentTableConfig(
  toRef(props, 'items'),
)
</script>

<template>
  <SimpleTable
    table-key="daily-preparation-assignment-list"
    item-key="id"
    :items="rows"
    :columns="columns"
    :filter-rules="filterRules"
    @update:items="emit('update:items', $event)"
  />
</template>