<script setup lang="ts" generic="T">
import type { MultiLevelHeaderLeafColumn, MultiLevelHeaderTableDef } from '../types/item/types'
import { useMultiLevelHeaderFilter } from '@/shared/components/table/multi_level_header_table/composables/useMultiLevelHeaderTableFilter'

const props = defineProps<{
  tableKey: string
  rowDef: MultiLevelHeaderTableDef<T>['row']
  columns: MultiLevelHeaderLeafColumn<T>[]
}>()

const { setFilter, filters } = useMultiLevelHeaderFilter(props.tableKey)

const getValue = (key: string) => filters.value[key]
const update = (key: string, value: unknown) => {
  setFilter(key, value)
}
</script>

<template>
  <tr class="mlh-filter-row">
    <!-- row header空 -->
    <th class="mlh-filter-sticky">
      <div class="mlh-filter-inner">
        <v-text-field
          v-if="rowDef.filter?.type === 'text'"
          density="compact"
          hide-details
          placeholder="検索"
          clearable
          :model-value="getValue(String(rowDef.key))"
          @update:model-value="(v) => update(String(rowDef.key), v)"
        />
      </div>
    </th>

    <th v-for="col in columns" :key="String(col.key)" class="mlh-filter-cell">
      <div class="mlh-filter-inner">
        <!-- text -->
        <v-text-field
          v-if="col.filter?.type === 'text'"
          variant="outlined"
          density="compact"
          hide-details
          placeholder="検索"
          clearable
          :model-value="getValue(String(col.key))"
          @update:model-value="(v) => update(String(col.key), v)"
        />

        <!-- number -->
        <v-text-field
          v-else-if="col.filter?.type === 'number'"
          type="number"
          variant="outlined"
          density="compact"
          hide-details
          placeholder="数値"
          clearable
          :model-value="getValue(String(col.key))"
          @update:model-value="(v) => update(String(col.key), Number(v))"
        />

        <!-- select -->
        <v-select
          v-else-if="col.filter?.type === 'select'"
          :items="col.enumOptions"
          item-title="title"
          item-value="value"
          variant="outlined"
          density="compact"
          hide-details
          clearable
          placeholder="選択"
          :model-value="getValue(String(col.key))"
          @update:model-value="(v) => update(String(col.key), v)"
        />

        <!-- checkbox -->
        <v-checkbox
          v-else-if="col.filter?.type === 'checkbox'"
          density="compact"
          hide-details
          :model-value="getValue(String(col.key)) ?? null"
          @update:model-value="(v) => update(String(col.key), v)"
        />

        <!-- date -->
        <v-text-field
          v-else-if="col.filter?.type === 'date'"
          type="date"
          variant="outlined"
          density="compact"
          hide-details
          clearable
          :model-value="getValue(String(col.key))"
          @update:model-value="(v) => update(String(col.key), v)"
        />
      </div>
    </th>
  </tr>
</template>
