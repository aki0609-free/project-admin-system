<script setup lang="ts">
import { toRef } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import type { DailyReportResponse } from '@/features/dailyreport/types/dailyReportApiTypes'
import {
  type DailyReportTableRow,
  useDailyReportTableConfig,
} from '@/features/dailyreport/composables/useDailyReportTableConfig'

const props = defineProps<{
  items: DailyReportResponse[]
}>()

const emit = defineEmits<{
  (e: 'row-click', row: DailyReportTableRow): void
}>()

const {
  rows,
  columns,
  filterRules,
} = useDailyReportTableConfig(toRef(props, 'items'))
</script>

<template>
  <SimpleTable
    table-key="daily-report-list"
    item-key="id"
    :items="rows"
    :columns="columns"
    :filter-rules="filterRules"
    enable-row-click
    @row-click="emit('row-click', $event)"
  />
</template>