<script setup lang="ts">
import { toRef } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import type { DailyReportMissingEmployeeResponse } from '@/features/dailyreport/types/dailyReportApiTypes'
import {
  type DailyReportMissingEmployeeTableRow,
  useDailyReportMissingEmployeeTableConfig,
} from '@/features/dailyreport/composables/useDailyReportMissingEmployeeTableConfig'

const props = defineProps<{
  items: DailyReportMissingEmployeeResponse[]
}>()

const emit = defineEmits<{
  (e: 'row-click', row: DailyReportMissingEmployeeTableRow): void
}>()

const {
  rows,
  columns,
  filterRules,
} = useDailyReportMissingEmployeeTableConfig(toRef(props, 'items'))
</script>

<template>
  <SimpleTable
    table-key="daily-report-missing-employee-list"
    item-key="id"
    :items="rows"
    :columns="columns"
    :filter-rules="filterRules"
    enable-row-click
    @row-click="emit('row-click', $event)"
  />
</template>