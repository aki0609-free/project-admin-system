<script setup lang="ts">
import { toRef } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import {
  useDailyPreparationDispatchTableConfig,
  type DailyPreparationDispatchTableRow,
} from '../composables/useDailyPreparationDispatchTableConfig'

const props = defineProps<{
  items: DailyPreparationDispatchTableRow[]
}>()

const emit = defineEmits<{
  (
    e: 'update:items',
    payload: {
      id: number
      field: keyof DailyPreparationDispatchTableRow
      value: unknown
    }
  ): void
}>()

const {
  rows,
  columns,
  filterRules,
} = useDailyPreparationDispatchTableConfig(
  toRef(props, 'items'),
)
</script>

<template>
  <SimpleTable
    table-key="daily-preparation-dispatch-list"
    item-key="id"
    :items="rows"
    :columns="columns"
    :filter-rules="filterRules"
    @update:items="emit('update:items', $event)"
  />
</template>