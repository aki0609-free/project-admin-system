<script setup lang="ts">
import { toRef } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import type { DailyReportMonthlyAttendanceResponse } from '@/features/dailyreport/types/dailyReportApiTypes'
import {
  type DailyReportMonthlyAttendanceTableRow,
  useDailyReportMonthlyAttendanceTableConfig,
} from '@/features/dailyreport/composables/useDailyReportMonthlyAttendanceTableConfig'

const props = defineProps<{
  items: DailyReportMonthlyAttendanceResponse[]
}>()

const emit = defineEmits<{
  (e: 'row-click', row: DailyReportMonthlyAttendanceTableRow): void
}>()

const { rows, columns, filterRules } =
  useDailyReportMonthlyAttendanceTableConfig(toRef(props, 'items'))
</script>

<template>
  <SimpleTable
    table-key="daily-report-monthly-attendance-list"
    item-key="id"
    :items="rows"
    :columns="columns"
    :filter-rules="filterRules"
    enable-row-click
    @row-click="emit('row-click', $event)"
  />
</template>