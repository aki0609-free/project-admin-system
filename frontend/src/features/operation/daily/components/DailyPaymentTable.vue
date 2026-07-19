<script setup lang="ts">
import { toRef } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import {
  useDailyPaymentTableConfig,
  type DailyPaymentTableRow,
} from '../composables/useDailyPaymentTableConfig'

const props = defineProps<{
  items: DailyPaymentTableRow[]
}>()

const emit = defineEmits<{
  (
    e: 'update:items',
    payload: {
      id: number
      field: keyof DailyPaymentTableRow
      value: unknown
    }
  ): void
}>()

const {
  rows,
  columns,
  filterRules,
} = useDailyPaymentTableConfig(
  toRef(props, 'items'),
)
</script>

<template>
  <SimpleTable
    table-key="daily-payment-table"
    item-key="id"
    :items="rows"
    :columns="columns"
    :filter-rules="filterRules"
    @update:items="emit('update:items', $event)"
  />
</template>