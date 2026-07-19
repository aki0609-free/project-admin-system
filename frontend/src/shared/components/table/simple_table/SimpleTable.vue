<template>
  <v-card>
    <div class="scroll-container">
      <!-- ===== Filter ===== -->
      <SimpleTableFilter
        :table-key="tableKey"
        :columns="columns"
      />
      <!-- ===== Table ===== -->
      <v-data-table
        v-model:items-per-page="itemsPerPage"
        v-model:page="page"
        :headers="tableColumns"
        :items="filteredItems"
        :item-key="itemKey"
        density="compact"
        class="data-table"
        @click:row="handleRowClick"
      >
        <template
          v-for="col in columns"
          #[`item.${String(col.key)}`]="{ item }"
          :key="`${item.id}-${String(col.key)}`"
        >
          <slot
            v-if="$slots[`item.${String(col.key)}`]"
            :name="`item.${String(col.key)}`"
            :item="item"
          />

          <EditingCell
            v-else
            :item="item"
            :field="col.key"
            :column="col"
            :editing-cell="editingCell"
            @update:editing-cell="editingCell = $event"
            @update="onCellUpdate"
          />
        </template>
                
        <!-- ===== Total ===== -->
        <template #bottom>
            <SimpleTableTotal
                :filtered-items="computed(() => filteredItems)"
            />
        </template>
      </v-data-table>
    </div>
  </v-card>
</template>

<script setup lang="ts" generic="T extends { id: number }">
import { computed, reactive, ref } from 'vue'
import SimpleTableFilter from './ui/SimpleTableFilter.vue'
import { type SimpleTableColumnDef } from '@/shared/components/table/simple_table/types/item/types'
import SimpleTableTotal from './ui/SimpleTableTotal.vue'
import type { SimpleTableFilterRule } from '@/shared/components/table/simple_table/types/filter/types'
import { useSimpleTablePagination } from '@/shared/components/table/simple_table/composables/useSimpleTablePagination'
import { useSimpleTableFilter } from '@/shared/components/table/simple_table/composables/useSimpleTableFilter'
import EditingCell from './ui/SimpleTableEditingCell.vue'

// Props & Emit
const props = defineProps<{
  tableKey: string
  itemKey: string
  items: T[]
  columns: SimpleTableColumnDef<T>[]
  filterRules: SimpleTableFilterRule<T>[]
  enableRowClick?: boolean
}>()
const emit = defineEmits<{
  (e: 'update:items', payload: { id: number; field: keyof T; value: unknown }): void
  (e: 'row-click', row: T): void
}>()
defineSlots<{
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  [key: `item.${string}`]: (props: { item: T }) => any
}>()

// Emit Functions
const onCellUpdate = ({id, field, value}: {id: number, field: keyof T, value: unknown}): void => {
  emit('update:items', {id: id, field: field, value: value})
}
const handleRowClick = (_: unknown, ctx: { item: T }) => {
  if (!props.enableRowClick) return

  emit('row-click', ctx.item)
}

// composable & store
const { page, itemsPerPage } = useSimpleTablePagination(props.tableKey)
const { applyFilter } = useSimpleTableFilter(props.tableKey)

// constants
const editingCell = ref<{ id: number; key: string } | null>(null)

// computed Functions
const filteredItems = computed(() => applyFilter(props.items, props.filterRules))

const tableColumns = reactive(props.columns.map(col => ({
  title: col.title,
  key: String(col.key),
  width: col.width ?? 160,
})))
</script>

<style scoped>
.scroll-container {
  overflow-x: auto;
  overflow-y: auto;  /* 追加 */
  width: 100%;
  height: 100%;      /* v-card の高さに合わせる */
  box-sizing: border-box;
  padding: 16px;     /* v-card の padding を考慮 */
}

.data-table {
  min-width: max-content;
}

/* ヘッダー */
.data-table :deep(thead th) {
  background-color: #f0f3f7;
  color: #333;
  font-weight: 600;
  border-right: 1px solid #dcdcdc;
  white-space: nowrap;
}

/* ヘッダー最後の線消す */
.data-table :deep(thead th:last-child) {
  border-right: none;
  margin-top: 0;
}

/* ボディ */
.data-table :deep(tbody td) {
  border-right: 1px solid #e0e0e0;
  white-space: nowrap;
}

/* ボディ最後の線消す */
.data-table :deep(tbody td:last-child) {
  border-right: none;
}

.table-toolbar {
  position: sticky;
  left: 0;
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background: #fafafa;
  border-top: 1px solid #e0e0e0;
}

.summary-box {
  display: flex;
  align-items: baseline;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 20px;
  background: #f0f3f7;
}

.summary-label {
  font-size: 12px;
  color: #666;
}

.summary-value {
  font-size: 20px;
  font-weight: 600;
  color: #1976d2;
}

.summary-unit {
  font-size: 12px;
  color: #888;
}
</style>
