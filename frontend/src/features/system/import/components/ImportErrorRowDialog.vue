<script setup lang="ts">
import { computed } from 'vue'

import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import type {
  SimpleTableColumnDef,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'

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
  <v-dialog
    v-model="visible"
    max-width="1200"
    scrollable
  >
    <v-card rounded="xl">
      <v-card-title class="dialog-title">
        Import Error Rows
      </v-card-title>

      <v-divider />

      <v-card-text class="dialog-body">
        <SimpleTable
          table-key="import-error-rows"
          item-key="id"
          :items="query.errors.value"
          :columns="columns"
          :filter-rules="filterRules"
        />
      </v-card-text>

      <v-divider />

      <v-card-actions class="justify-end">
        <v-btn
          variant="text"
          @click="visible = false"
        >
          閉じる
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.dialog-title {
  font-weight: 700;
  padding: 16px 20px;
}

.dialog-body {
  padding: 16px 20px;
}
</style>