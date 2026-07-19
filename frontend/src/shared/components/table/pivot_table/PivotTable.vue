<script setup lang="ts" generic="T extends Record<string, any>">
import { computed, ref } from 'vue'
import { usePivot } from '@/shared/components/table/pivot_table/composables/usePivot'
import { usePivotTableFilter } from '@/shared/components/table/pivot_table/composables/usePivotTableFilter'
import { usePivotSort } from './composables/usePivotSort'
import { usePivotHeatmap } from '@/shared/components/table/pivot_table/composables/usePivotHeatMap'
import { useDrillDown } from './composables/useDrillDown'
import type { PivotTableDef } from '@/shared/components/table/pivot_table/types/item/types'

import PivotToolbar from './ui/PivotToolbar.vue'
import PivotTableHeader from './ui/PivotTableHeader.vue'
import PivotTableBody from './ui/PivotTableBody.vue'
import PivotTableFooter from './ui/PivotTableFooter.vue'

import './css/styles.css'
import PivotTableFilter from './ui/PivotTableFilter.vue'
import { createPivotFilterRules } from '@/shared/components/table/pivot_table/utils/createPivotFilterRules'

const props = defineProps<{
  data: T[]
  def: PivotTableDef<T>
  tableKey?: string
}>()

const tableKey = props.tableKey ?? 'default'

// UI State
const currentAggregation = ref<'sum' | 'avg' | 'count'>('sum')
const sortType = ref<'none' | 'desc' | 'asc'>('none')

// Filter
const { applyFilter } = usePivotTableFilter(tableKey)
const rules = computed(() => createPivotFilterRules(props.def))

const filteredData = computed(() => {
  return applyFilter(props.data, rules.value)
})

// Pivot
const pivot = computed(() => {
  const { row, column, values } = props.def
  const value = values[0]

  return usePivot(filteredData.value, {
    rowKey: row.key,
    columnKey: column.key,
    valueKey: value!.key,
    aggregation: currentAggregation.value,

    rowFormatter: (v) => row.formatter?.(v, {} as T) ?? String(v),
    columnFormatter: (v) => column.formatter?.(v, {} as T) ?? String(v),

    showRowTotals: true,
    showColumnTotals: true,
  })
})

// composables
const sortedRowIndexes = usePivotSort(
  computed(() => pivot.value.rowKeys),
  computed(() => pivot.value.rowTotals),
  sortType,
)

const heatmap = usePivotHeatmap(
  computed(() => pivot.value.matrix)
)

const drill = useDrillDown<T>()

// Event
function handleCellClick(payload: { row: string; col: string }) {
  drill.open(
    props.data,
    props.def.row.key,
    props.def.column.key,
    payload.row,
    payload.col
  )
}
</script>

<template>
  <div class="pivot-root">
    <!-- Toolbar -->
    <PivotToolbar
      :aggregation="currentAggregation"
      :sort="sortType"
      @update:aggregation="(v) => (currentAggregation = v)"
      @update:sort="(v) => (sortType = v)"
    >
      <template #title>
        {{ def.row.title }} × {{ def.column.title }}
      </template>

      <template #actions>
        <PivotTableFilter
          :def="def"
          :data="data"
          :table-key="tableKey"
        />
      </template>
    </PivotToolbar>

    <!-- Table -->
    <div class="pivot-wrapper">
      <table class="pivot-table">
        <PivotTableHeader
          :row-title="def.row.title"
          :column-keys="pivot.columnKeys"
          :show-totals="pivot.showRowTotals"
        />

        <PivotTableBody
          :row-keys="pivot.rowKeys"
          :column-keys="pivot.columnKeys"
          :matrix="pivot.matrix"
          :row-totals="pivot.rowTotals"
          :sorted-indexes="sortedRowIndexes"
          :show-totals="pivot.showRowTotals"
          :get-color="heatmap.getColor"
          @cell-click="handleCellClick"
        />

        <PivotTableFooter
          v-if="pivot.showColumnTotals"
          :column-keys="pivot.columnKeys"
          :column-totals="pivot.columnTotals"
          :grand-total="pivot.grandTotal"
          :show-row-totals="pivot.showRowTotals"
        />
      </table>
    </div>

    <!-- DrillDown -->
    <v-dialog v-model="drill.dialog.value" width="900">
      <v-card>
        <v-card-title>明細データ</v-card-title>

        <v-card-text>
          <v-data-table
            :items="drill.data.value"
            density="compact"
          />
        </v-card-text>
      </v-card>
    </v-dialog>
  </div>
</template>