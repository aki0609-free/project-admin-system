<script setup lang="ts">
import { toRef } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import type { EmployeeListItemResponse } from '../types/employeeApiTypes'
import {
  type EmployeeTableRow,
  useEmployeeTableConfig,
} from '../composables/useEmployeeTableConfig'

const props = defineProps<{
  items: EmployeeListItemResponse[]
}>()

const emit = defineEmits<{
  (e: 'row-click', row: EmployeeTableRow): void
}>()

const {
  rows,
  columns,
  filterRules,
} = useEmployeeTableConfig(toRef(props, 'items'))
</script>

<template>
  <SimpleTable
    table-key="employee-list"
    item-key="id"
    :items="rows"
    :columns="columns"
    :filter-rules="filterRules"
    enable-row-click
    @row-click="emit('row-click', $event)"
  />
</template>