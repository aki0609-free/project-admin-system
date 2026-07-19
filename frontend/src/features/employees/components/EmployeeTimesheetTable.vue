<script setup lang="ts">
import { toRef } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import { EmployeeTimesheetTableRow, useEmployeeTimesheetTableConfig } from '@/features/employees/composables/useEmployeeTimesheetTableConfig';
import { EmployeeTimesheetResponse } from '@/features/employees/types/employeeWorkApiTypes';

const props = defineProps<{
  items: EmployeeTimesheetResponse[]
}>()

const emit = defineEmits<{
  (e: 'row-click', row: EmployeeTimesheetTableRow): void
}>()

const {
  rows,
  columns,
  filterRules,
} = useEmployeeTimesheetTableConfig(toRef(props, 'items'))
</script>

<template>
  <SimpleTable
    tableKey="employee-timesheet-list"
    itemKey="id"
    :items="rows"
    :columns="columns"
    :filterRules="filterRules"
    enableRowClick
    @row-click="emit('row-click', $event)"
  />
</template>