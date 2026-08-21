<script setup lang="ts">
import { computed } from 'vue'

import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import type {
  SimpleTableColumnDef,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import AppDialog from '@/shared/ui/dialog/AppDialog.vue'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'

import { useImportErrorRowsQuery } from '@/features/system/import/api/useImportErrorRowsQuery'
import type { ImportErrorRowResponse } from '@/features/system/import/types/importApiTypes'

const props = defineProps<{
  modelValue: boolean
  historyId: number | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})

const historyId = computed(() => props.historyId)

const query = useImportErrorRowsQuery(historyId)

const rightFooterItems = computed<ToolbarItem[]>(() => [
  {
    id: 'close',
    type: 'button',
    label: '閉じる',
    intent: 'secondary',
    onClick: () => {
      visible.value = false
    },
  },
])

const columns = computed(() => {
  const defs: SimpleTableColumnDef<ImportErrorRowResponse>[] = [
    {
      title: 'Row',
      key: 'rowNo',
      type: 'number',
      width: '100px',
      filter: { type: 'number' },
    },
    {
      title: 'Header',
      key: 'csvHeaderName',
      type: 'text',
      width: '180px',
      filter: { type: 'text' },
    },
    {
      title: 'Column',
      key: 'columnName',
      type: 'text',
      width: '180px',
      filter: { type: 'text' },
    },
    {
      title: 'Value',
      key: 'rawValue',
      type: 'text',
      width: '260px',
      filter: { type: 'text' },
    },
    {
      title: 'Error',
      key: 'errorMessage',
      type: 'text',
      width: '520px',
      filter: { type: 'text' },
    },
  ]

  return defs
})

const filterRules = computed(() =>
  createSimpleTableFilterRules<ImportErrorRowResponse>(columns.value),
)
</script>

<template>
  <AppDialog
    v-model="visible"
    title="インポートエラー行"
    size="xl"
    :max-width="1200"
    :right-footer-items="rightFooterItems"
  >
    <SimpleTable
      table-key="import-error-rows"
      item-key="id"
      :items="query.errors.value"
      :columns="columns"
      :filter-rules="filterRules"
    />
  </AppDialog>
</template>
