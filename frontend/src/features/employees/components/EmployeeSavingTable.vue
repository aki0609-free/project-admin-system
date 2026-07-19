<script setup lang="ts">
import { toRef } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import { type EmployeeSavingTableRow, useEmployeeSavingTableConfig } from '@/features/employees/composables/useEmployeeSavingTableConfig';
import type { EmployeeSavingResponse } from '@/features/employees/types/employeeWorkApiTypes';

const props = defineProps<{
  items: EmployeeSavingResponse[]
}>()

const emit = defineEmits<{
  (e: 'row-click', row: EmployeeSavingTableRow): void
}>()

const {
  rows,
  columns,
  filterRules,
} = useEmployeeSavingTableConfig(toRef(props, 'items'))
</script>

<template>
  <SimpleTable
    table-key="employee-saving-list"
    item-key="id"
    :items="rows"
    :columns="columns"
    :filter-rules="filterRules"
    enable-row-click
    @row-click="emit('row-click', $event)"
  />
</template>