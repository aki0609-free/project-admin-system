<script setup lang="ts">
import { toRef } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import { useEmployeeFinanceTransactionTableConfig } from '../composables/useEmployeeFinanceTransactionTableConfig'
import type { EmployeeFinanceTransactionResponse } from '../types/employeeWorkApiTypes'

const props = defineProps<{
  items: EmployeeFinanceTransactionResponse[]
}>()

const { rows, columns, filterRules } = useEmployeeFinanceTransactionTableConfig(
  toRef(props, 'items'),
)
</script>

<template>
  <SimpleTable
    table-key="employee-finance-transaction-list"
    item-key="id"
    :items="rows"
    :columns="columns"
    :filter-rules="filterRules"
  />
</template>
