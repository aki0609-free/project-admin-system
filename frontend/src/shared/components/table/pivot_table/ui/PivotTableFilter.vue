<script setup lang="ts" generic="T extends Record<string, any>">
import { usePivotTableFilter } from '../composables/usePivotTableFilter'
import type { PivotTableDef } from '../types/item/types'

const props = defineProps<{
  def: PivotTableDef<T>
  tableKey: string
  data: T[]
}>()

const { setFilter, reset } = usePivotTableFilter(props.tableKey)

// 値候補（ユニーク）
function getOptions(key: keyof T) {
  return [...new Set(props.data.map((d) => d[key]))]
}
</script>

<template>
  <div class="pivot-filter">
    <div class="filter-header">
      <v-btn size="small" variant="text" @click="reset">リセット</v-btn>
    </div>

    <div class="filter-group filter-row">
      <div class="filter-title">{{ def.row.title }}</div>

      <v-select
        :items="getOptions(def.row.key)"
        clearable
        density="compact"
        @update:modelValue="(v) => setFilter('row', def.row.key as string, v)"
      />
    </div>

    <div class="filter-group filter-column">
      <div class="filter-title">{{ def.column.title }}</div>

      <v-select
        :items="getOptions(def.column.key)"
        clearable
        density="compact"
        @update:modelValue="(v) => setFilter('column', def.column.key as string, v)"
      />
    </div>
  </div>
</template>