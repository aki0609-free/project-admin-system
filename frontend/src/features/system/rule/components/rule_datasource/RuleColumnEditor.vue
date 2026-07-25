<script setup lang="ts">
import { computed } from 'vue'

import type { RuleColumnMappingForm } from '@/features/system/rule/types/ruleFormTypes'
import type {
  RuleDataSourceCatalogColumn,
  RuleDataType,
} from '../../types/ruleApiTypes'

const props = defineProps<{
  column: RuleColumnMappingForm
  availableColumns: RuleDataSourceCatalogColumn[]
}>()

const emit = defineEmits<{
  selectColumn: [columnName: string, dataType: RuleDataType]
}>()

const dataTypeItems = [
  'STRING',
  'INTEGER',
  'LONG',
  'DECIMAL',
  'BOOLEAN',
  'DATE',
  'DATETIME',
]

const columnItems = computed(() =>
  props.availableColumns.map(item => ({
    title: item.displayName,
    value: item.columnName,
    dataType: item.dataType,
  })),
)

const selectColumn = (columnName: string) => {
  const selected = props.availableColumns.find(
    item => item.columnName === columnName,
  )

  if (selected) {
    emit(
      'selectColumn',
      selected.columnName,
      selected.dataType,
    )
  }
}
</script>

<template>
  <div class="column-editor-card">
    <template v-if="column">
      <div class="card-title">
        Column Detail
      </div>

      <div class="column-editor-grid">
        <v-select
          v-model="column.columnName as string"
          label="columnName"
          :items="columnItems"
          variant="outlined"
          density="comfortable"
          @update:model-value="selectColumn"
        />

        <v-text-field
          v-model="column.factKey as string"
          label="factKey"
          variant="outlined"
          density="comfortable"
        />

        <v-select
          v-model="column.dataType as RuleDataType"
          label="dataType"
          :items="dataTypeItems"
          disabled
          variant="outlined"
          density="comfortable"
        />

        <v-text-field
          v-model.number="column.orderNo as number"
          label="orderNo"
          type="number"
          variant="outlined"
          density="comfortable"
        />

        <v-checkbox
          v-model="column.requiredFlag as boolean"
          label="required"
          density="compact"
          hide-details
        />
      </div>
    </template>

    <div
      v-else
      class="empty-card"
    >
      Columnを選択してください。
    </div>
  </div>
</template>

<style scoped>
.column-editor-card {
  display: grid;
  gap: 14px;
  align-content: start;
  min-width: 0;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #ffffff;
}

.card-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.column-editor-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.empty-card {
  min-height: 260px;
  display: grid;
  place-items: center;
  color: #64748b;
}

@media (max-width: 900px) {
  .column-editor-grid {
    grid-template-columns: 1fr;
  }
}
</style>
