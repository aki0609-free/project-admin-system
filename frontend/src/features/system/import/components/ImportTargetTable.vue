<script setup lang="ts">
import { computed } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import type { SimpleTableFilterRule } from '@/shared/components/table/simple_table/types/filter/types'
import type { ImportTargetResponse } from '@/features/system/import/types/importApiTypes'

type Row = SimpleTableEditableRow & {
  id: number
  targetCode: string
  targetName: string
  tableName: string
  sourceType: string
  importMode: string
  activeText: string
  description: string
  raw: ImportTargetResponse
}

const props = defineProps<{
  items: ImportTargetResponse[]
}>()

const emit = defineEmits<{
  (e: 'row-click', row: ImportTargetResponse): void
}>()

const rows = computed<Row[]>(() =>
  props.items.map((item) => ({
    id: item.id,
    targetCode: item.targetCode,
    targetName: item.targetName,
    tableName: item.tableName,
    sourceType: item.sourceType,
    importMode: item.importMode,
    activeText: item.activeFlag ? '有効' : '無効',
    description: item.description ?? '',
    raw: item,
  })),
)

const columns: SimpleTableColumnDef<Row>[] = [
  { title: 'targetCode', key: 'targetCode', type: 'text', width: '400px', filter: { type: 'text' } },
  { title: 'targetName', key: 'targetName', type: 'text', width: '300px', filter: { type: 'text' } },
  { title: 'tableName', key: 'tableName', type: 'text', width: '300px', filter: { type: 'text' } },
  { title: 'sourceType', key: 'sourceType', type: 'text', width: '140px', filter: { type: 'text' } },
  { title: 'importMode', key: 'importMode', type: 'text', width: '140px', filter: { type: 'text' } },
  { title: '状態', key: 'activeText', type: 'text', width: '100px', filter: { type: 'text' } },
  { title: '説明', key: 'description', type: 'text', width: '600px', filter: { type: 'text' } },
]

const filterRules: SimpleTableFilterRule<Row>[] = columns.map((col) => ({
  key: col.key,
  predicate: (item, value) => String(item[col.key] ?? '').includes(String(value ?? '')),
}))

const onRowClick = (row: Row) => {
  emit('row-click', row.raw)
}
</script>

<template>
  <SimpleTable
    tableKey="import-target-list"
    itemKey="id"
    :items="rows"
    :columns="columns"
    :filterRules="filterRules"
    enableRowClick
    @row-click="onRowClick"
  />
</template>