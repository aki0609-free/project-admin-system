<template>
  <div v-if="tableState?.enabled" class="filter-row">
    <div v-for="col in columns" :key="String(col.key)" class="cell filter-cell">
      <!-- text -->
      <v-text-field
        v-if="col.filter?.type === 'text'"
        :model-value="tableState.filters[col.key as string]"
        @update:model-value="(val) => setFilter(col.key as string, val)"
        :label="col.title"
        density="compact"
        variant="underlined"
        hide-details
        clearable
        class="filter-control w-100"
      />

      <!-- select -->
      <v-select
        v-else-if="col.filter?.type === 'select'"
        :model-value="tableState.filters[col.key as string]"
        @update:model-value="(val) => setFilter(col.key as string, val)"
        :items="col.enumOptions"
        item-title="title"
        item-value="value"
        :label="col.title"
        density="compact"
        variant="underlined"
        hide-details
        clearable
        class="filter-control w-100"
      />

      <!-- checkbox -->
      <v-checkbox
        v-else-if="col.filter?.type === 'checkbox'"
        :model-value="tableState.filters[col.key as string]"
        @update:model-value="(val) => setFilter(col.key as string, val)"
        density="compact"
        hide-details
        class="w-100 pl-3"
      />

      <!-- date -->
      <v-menu
        v-else-if="col.filter?.type === 'date'"
        v-model="dateMenus[String(col.key)]"
        :close-on-content-click="false"
        transition="scale-transition"
        offset-y
        min-width="auto"
      >
        <template #activator="{ props: menuProps }">
          <v-text-field
            v-bind="menuProps"
            :model-value="formatYearMonthDay(tableState.filters[col.key as string] as string)"
            @click:clear="setFilter(col.key as string, '')"
            :label="col.title"
            density="compact"
            variant="underlined"
            hide-details
            clearable
            readonly
            class="filter-control w-100"
          />
        </template>

        <v-locale-provider locale="ja">
          <v-date-picker
            :model-value="tableState?.filters[col.key as string] as string | undefined"
            @update:model-value="(val) => handleDateFilterUpdate(col.key as string, val)"
            title="日付を選択"
            header="日付を選択"
          />
        </v-locale-provider>
      </v-menu>
    </div>
  </div>
</template>

<script setup lang="ts" generic="T extends SimpleTableEditableRow">
import { useSimpleTableFilterStore } from '@/shared/components/table/simple_table/store/filter/useSimpleTableFilterStore'
import {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import {
  formatDatePickerHeader,
  formatDatePickerMonth,
} from '@/shared/components/table/simple_table/utils/localDateUtils'
import { formatYearMonthDay, normalizeDateValue } from '@/shared/utils/DateUtils'
import { computed, onMounted, reactive } from 'vue'

const props = defineProps<{
  tableKey: string
  columns: SimpleTableColumnDef<T>[]
}>()

const filterStore = useSimpleTableFilterStore()

onMounted(() => {
  filterStore.init(props.tableKey)
})

const tableState = computed(() => filterStore.tables[props.tableKey])

const dateMenus = reactive<Record<string, boolean>>({})

const DEFAULT_COLUMN_WIDTH = 160
const getColumnWidth = (width?: string) => {
  if (!width) return DEFAULT_COLUMN_WIDTH

  const parsed = Number.parseFloat(width)
  return Number.isFinite(parsed) ? parsed : DEFAULT_COLUMN_WIDTH
}

const gridTemplateColumns = computed(() =>
  props.columns
    .map((column) => {
      const width = getColumnWidth(column.width)
      return `minmax(${width}px, ${width}fr)`
    })
    .join(' '),
)

const setFilter = (key: string, value: unknown) => {
  filterStore.setFilter(props.tableKey, key, value)
}

const handleDateFilterUpdate = (key: string, value: unknown) => {
  const normalized = normalizeDateValue(value)
  setFilter(key, normalized)
  dateMenus[key] = false
}
</script>

<style scoped>
.filter-row {
  display: grid;
  grid-template-columns: v-bind(gridTemplateColumns);
  width: 100%;
}

.filter-cell {
  padding: 4px;
  border-right: 1px solid #ddd;
  border-bottom: 1px solid #ddd;
  min-width: 0;
  height: 48px;
  overflow: hidden;
}

.filter-cell:last-child {
  border-right: none;
}

.filter-control :deep(.v-field) {
  min-height: 40px;
  height: 40px;
}

.filter-control :deep(.v-field__input) {
  min-height: 40px;
  padding-top: 0;
  padding-bottom: 0;
}
</style>
