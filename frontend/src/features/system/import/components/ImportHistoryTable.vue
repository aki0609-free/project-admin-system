<script setup lang="ts">
import { computed } from 'vue'

import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'

import type {
  ImportHistoryResponse,
  ImportHistoryStatus,
} from '@/features/system/import/types/importApiTypes'

type ImportHistoryTableRow = SimpleTableEditableRow & {
  id: number
  targetCode: string
  targetName: string
  fileName: string
  status: ImportHistoryStatus
  sourceType: string
  importMode: string
  totalCount: number
  insertedCount: number
  updatedCount: number
  skippedCount: number
  errorCount: number
  executedAt: string
  errorMessage: string
  raw: ImportHistoryResponse
}

const props = defineProps<{
  items: ImportHistoryResponse[]
}>()

const emit = defineEmits<{
  (e: 'show-errors', historyId: number): void
}>()

const rows = computed<ImportHistoryTableRow[]>(() =>
  props.items.map(item => ({
    id: item.id,
    targetCode: item.targetCode,
    targetName: item.targetName ?? '',
    fileName: item.fileName ?? '',
    status: item.status,
    sourceType: item.sourceType ?? '',
    importMode: item.importMode ?? '',
    totalCount: item.totalCount,
    insertedCount: item.insertedCount,
    updatedCount: item.updatedCount,
    skippedCount: item.skippedCount,
    errorCount: item.errorCount,
    executedAt: item.executedAt,
    errorMessage: item.errorMessage ?? '',
    raw: item,
  })),
)

const columns = computed(() => {
  const defs: SimpleTableColumnDef<ImportHistoryTableRow>[] = [
    {
      title: '状態',
      key: 'status',
      type: 'text',
      width: '150px',
      filter: { type: 'text' },
    },
    {
      title: 'targetCode',
      key: 'targetCode',
      type: 'text',
      width: '180px',
      filter: { type: 'text' },
    },
    {
      title: 'targetName',
      key: 'targetName',
      type: 'text',
      width: '220px',
      filter: { type: 'text' },
    },
    {
      title: 'fileName',
      key: 'fileName',
      type: 'text',
      width: '260px',
      filter: { type: 'text' },
    },
    {
      title: 'source',
      key: 'sourceType',
      type: 'text',
      width: '140px',
      filter: { type: 'text' },
    },
    {
      title: 'mode',
      key: 'importMode',
      type: 'text',
      width: '150px',
      filter: { type: 'text' },
    },
    {
      title: 'total',
      key: 'totalCount',
      type: 'number',
      width: '100px',
      filter: { type: 'text' },
    },
    {
      title: 'insert',
      key: 'insertedCount',
      type: 'number',
      width: '100px',
      filter: { type: 'text' },
    },
    {
      title: 'update',
      key: 'updatedCount',
      type: 'number',
      width: '100px',
      filter: { type: 'text' },
    },
    {
      title: 'skip',
      key: 'skippedCount',
      type: 'number',
      width: '100px',
      filter: { type: 'text' },
    },
    {
      title: 'error',
      key: 'errorCount',
      type: 'number',
      width: '100px',
      filter: { type: 'text' },
    },
    {
      title: 'executedAt',
      key: 'executedAt',
      type: 'text',
      width: '220px',
      filter: { type: 'text' },
    },
    {
      title: 'errorMessage',
      key: 'errorMessage',
      type: 'text',
      width: '320px',
      filter: { type: 'text' },
    },
  ]

  return defs
})

const filterRules = computed(() =>
  createSimpleTableFilterRules<ImportHistoryTableRow>(columns.value),
)

const onRowClick = (row: ImportHistoryTableRow) => {
  if (row.errorCount <= 0) return

  emit('show-errors', row.id)
}
</script>

<template>
  <SimpleTable
    table-key="import-history"
    item-key="id"
    :items="rows"
    :columns="columns"
    :filter-rules="filterRules"
    enable-row-click
    @row-click="onRowClick"
  />
</template>