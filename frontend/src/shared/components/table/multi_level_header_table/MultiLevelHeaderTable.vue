<script setup lang="ts" generic="T extends Record<string, any> & { id: number }">
import { computed, ref } from 'vue'
import { VDataTable } from 'vuetify/components'

import { buildHeaderRows } from '@/shared/components/table/multi_level_header_table/utils/buildHeaderRows'
import { getLeafColumns } from '@/shared/components/table/multi_level_header_table/utils/getLeafColumns'

import type {
  MultiLevelHeaderLeafColumn,
  MultiLevelHeaderTableDef,
} from '@/shared/components/table/multi_level_header_table/types/item/types'

import {
  getCellStyle,
  getHeaderStyle,
} from '@/shared/components/table/multi_level_header_table/utils/styleHelper'

import MultiLevelHeaderTableEditingCell from './ui/MultiLevelHeaderTableEditingCell.vue'
import MultiLevelHeaderTableFilter from './ui/MultiLevelHeaderTableFilter.vue'

import { createMultiLevelHeaderFilterRules } from './utils/createMultiLevelHeaderFilterRules'
import { useMultiLevelHeaderFilter } from '@/shared/components/table/multi_level_header_table/composables/useMultiLevelHeaderTableFilter'
import { useMultiLevelHeaderSort } from './composables/useMultiLevelHeaderSort'
import { useMultiLevelHeaderAggregation } from '@/shared/components/table/multi_level_header_table/composables/useMultiLevelHeaderAggregation'
import MultiLevelHeaderTableFooter from './ui/MultiLevelHeaderTableFooter.vue'
import { useMultiLevelHeaderPagination } from '@/shared/components/table/multi_level_header_table/composables/useMultiLevelHeaderPagination'

import './css/styles.css'

const props = defineProps<{
  data: T[]
  def: MultiLevelHeaderTableDef<T>
  tableKey?: string
}>()

const emit = defineEmits<{
  (e: 'update-cell', payload: { id: number; field: keyof T; value: unknown }): void
}>()

const tableKey = props.tableKey ?? 'mlh-table'

const headerRows = computed(() => buildHeaderRows(props.def.columns))

const leafColumns = computed<MultiLevelHeaderLeafColumn<T>[]>(() =>
  getLeafColumns(props.def.columns),
)

const filterRules = computed(() =>
  createMultiLevelHeaderFilterRules(leafColumns.value, props.def.row),
)

const { applyFilter } = useMultiLevelHeaderFilter(tableKey)
const { applySort, toggleSort, sortKey, sortOrder } = useMultiLevelHeaderSort(tableKey)

const items = computed(() => {
  const filtered = applyFilter(props.data, filterRules.value)
  return applySort(filtered)
})

const { summary } = useMultiLevelHeaderAggregation(
  () => items.value,
  () => leafColumns.value,
)

const editingCell = ref<{ id: number; key: string } | null>(null)

function handleUpdate({
  id,
  field,
  value,
}: {
  id: number
  field: keyof T
  value: unknown
}) {
  const row = props.data.find((r) => r.id === id)
  if (row) {
    row[field] = value as T[keyof T]
  }

  emit('update-cell', { id, field, value })
}

const { page, itemsPerPage } = useMultiLevelHeaderPagination(tableKey)
</script>

<template>
  <div class="mlh-table-wrapper">
    <v-data-table
      :headers="[]"
      :items="items"
      hide-default-header
      class="mlh-table"
      :page="page"
      :items-per-page="itemsPerPage"
    >
      <template #thead>
        <thead class="mlh-thead">
          <tr v-for="(row, i) in headerRows" :key="i">
            <th
              v-if="i === 0"
              :rowspan="headerRows.length"
              class="mlh-th mlh-row-header"
            >
              {{ def.row.title }}
            </th>

            <th
              v-for="col in row"
              :key="col.key"
              :colspan="col.colspan"
              :rowspan="col.rowspan"
              class="mlh-th"
              :class="{
                'mlh-group': col.isGroup,
                'mlh-leaf sortable': !col.isGroup,
              }"
              :style="getHeaderStyle(col)"
              @click="!col.isGroup && toggleSort(col.key)"
            >
              <div class="mlh-header-cell">
                {{ col.title }}
                <span v-if="sortKey === col.key">
                  {{ sortOrder === 'asc' ? '▲' : '▼' }}
                </span>
              </div>
            </th>
          </tr>

          <MultiLevelHeaderTableFilter
            :table-key="tableKey"
            :row-def="def.row"
            :columns="leafColumns"
            v-if="!leafColumns.filter"
          />
        </thead>
      </template>

      <template #item="{ item }">
        <tr>
          <td class="mlh-td mlh-row-header">
            {{ item[def.row.key] }}
          </td>

          <td
            v-for="col in leafColumns"
            :key="col.key"
            class="mlh-td"
            :style="getCellStyle(col)"
          >
            <MultiLevelHeaderTableEditingCell
              :item="item"
              :column="col"
              :editingCell="editingCell"
              @update="handleUpdate"
              @update:editingCell="editingCell = $event"
            />
          </td>
        </tr>
      </template>

      <template #tfoot>
        <MultiLevelHeaderTableFooter
          :summary="summary"
          :columns="leafColumns"
          row-title="合計"
        />
      </template>
    </v-data-table>
  </div>
</template>