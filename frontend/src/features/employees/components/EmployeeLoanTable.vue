<script setup lang="ts">
import { toRef } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import { EmployeeLoanResponse } from '@/features/employees/types/employeeWorkApiTypes';
import { EmployeeLoanTableRow, useEmployeeLoanTableConfig } from '@/features/employees/composables/useEmployeeLoanTableConfig';

const props = defineProps<{
  items: EmployeeLoanResponse[]
}>()

const emit = defineEmits<{
  (e: 'row-click', row: EmployeeLoanTableRow): void
}>()

const {
  rows,
  columns,
  filterRules,
} = useEmployeeLoanTableConfig(toRef(props, 'items'))
</script>

<template>
  <SimpleTable
    tableKey="employee-loan-list"
    itemKey="id"
    :items="rows"
    :columns="columns"
    :filterRules="filterRules"
    enableRowClick
    @row-click="emit('row-click', $event)"
  />
</template>