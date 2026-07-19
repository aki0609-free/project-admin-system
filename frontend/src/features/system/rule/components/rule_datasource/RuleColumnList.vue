<!-- eslint-disable vue/no-mutating-props -->
<script setup lang="ts">
import { computed } from 'vue'

import GenericToolbar from '@/shared/components/toolbar/GenericToolbar.vue'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import type {
  RuleColumnMappingForm,
  RuleDataSourceForm,
} from '@/features/system/rule/types/ruleFormTypes'
import { createEmptyRuleColumn } from '@/features/system/rule/mapper/ruleFormMapper'

const props = defineProps<{
  dataSource: RuleDataSourceForm | null
  selectedColumn: RuleColumnMappingForm | null
}>()

const emit = defineEmits<{
  select: [column: RuleColumnMappingForm | null]
}>()

const addColumn = () => {
  if (!props.dataSource) return

  const column = createEmptyRuleColumn(
    props.dataSource.columns.length + 1,
  )

  props.dataSource.columns.push(column)
  emit('select', column)
}

const removeColumn = () => {
  if (!props.dataSource || !props.selectedColumn) return

  props.dataSource.columns = props.dataSource.columns.filter(
    item => item.id !== props.selectedColumn?.id,
  )

  emit('select', null)
}

const toolbarItems = computed<ToolbarItem[]>(() => [
  {
    type: 'button',
    label: '追加',
    color: 'primary',
    icon: 'mdi-plus',
    disabled: !props.dataSource,
    onClick: addColumn,
  },
  {
    type: 'button',
    label: '削除',
    color: 'error',
    variant: 'text',
    icon: 'mdi-delete-outline',
    disabled: !props.selectedColumn,
    onClick: removeColumn,
  },
])
</script>

<template>
  <div class="column-list-card">
    <div class="card-header">
      <div>
        <div class="card-title">
          Columns
        </div>

        <div class="card-description">
          DBカラムをFactキーへ変換します。
        </div>
      </div>
    </div>

    <GenericToolbar :items="toolbarItems" />

    <div
      v-if="dataSource"
      class="column-list"
    >
      <button
        v-for="column in dataSource.columns"
        :key="column.id"
        type="button"
        class="column-item"
        :class="{ active: selectedColumn?.id === column.id }"
        @click="emit('select', column)"
      >
        <div class="column-name">
          {{ column.columnName || 'column未設定' }}
        </div>

        <div class="column-meta">
          {{ column.factKey || 'factKey未設定' }}
        </div>

        <div class="column-flags">
          <v-chip
            size="x-small"
            variant="tonal"
            color="blue-grey"
          >
            {{ column.dataType }}
          </v-chip>

          <v-chip
            v-if="column.requiredFlag"
            size="x-small"
            variant="tonal"
            color="error"
          >
            required
          </v-chip>
        </div>
      </button>

      <div
        v-if="dataSource.columns.length === 0"
        class="empty-list"
      >
        Columnを追加してください。
      </div>
    </div>

    <div
      v-else
      class="empty-list"
    >
      DataSourceを選択してください。
    </div>
  </div>
</template>

<style scoped>
.column-list-card {
  display: grid;
  gap: 12px;
  align-content: start;
  min-width: 0;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #f8fafc;
}

.card-title {
  font-weight: 700;
  color: #0f172a;
}

.card-description {
  margin-top: 2px;
  font-size: 12px;
  color: #64748b;
}

.column-list {
  display: grid;
  gap: 8px;
}

.column-item {
  display: grid;
  gap: 4px;
  width: 100%;
  padding: 10px 12px;
  text-align: left;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #ffffff;
  cursor: pointer;
}

.column-item:hover {
  background: #eff6ff;
}

.column-item.active {
  border-color: #2563eb;
  background: #dbeafe;
}

.column-name {
  font-weight: 700;
  color: #0f172a;
}

.column-meta {
  font-size: 12px;
  color: #64748b;
}

.column-flags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 4px;
}

.empty-list {
  min-height: 180px;
  display: grid;
  place-items: center;
  color: #64748b;
}
</style>