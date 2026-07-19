<!-- eslint-disable vue/no-mutating-props -->
<script setup lang="ts">
import { computed } from 'vue'

import GenericToolbar from '@/shared/components/toolbar/GenericToolbar.vue'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import type {
  RuleDataSourceForm,
  RuleMasterForm,
} from '@/features/system/rule/types/ruleFormTypes'
import { createEmptyRuleDataSource } from '@/features/system/rule/mapper/ruleFormMapper'

const props = defineProps<{
  form: RuleMasterForm
  selectedDataSource: RuleDataSourceForm | null
}>()

const emit = defineEmits<{
  select: [source: RuleDataSourceForm | null]
}>()

const addDataSource = () => {
  const source = createEmptyRuleDataSource(
    props.form.dataSources.length + 1,
  )

  props.form.dataSources.push(source)
  emit('select', source)
}

const removeDataSource = () => {
  if (!props.selectedDataSource) return

  props.form.dataSources = props.form.dataSources.filter(
    item => item.id !== props.selectedDataSource?.id,
  )

  emit('select', null)
}

const toolbarItems = computed<ToolbarItem[]>(() => [
  {
    type: 'button',
    label: '追加',
    color: 'primary',
    icon: 'mdi-plus',
    onClick: addDataSource,
  },
  {
    type: 'button',
    label: '削除',
    color: 'error',
    variant: 'text',
    icon: 'mdi-delete-outline',
    disabled: !props.selectedDataSource,
    onClick: removeDataSource,
  },
])
</script>

<template>
  <div class="source-list-card">
    <div class="card-header">
      <div>
        <div class="card-title">
          DataSource
        </div>

        <div class="card-description">
          Ruleで使用する取得元データ
        </div>
      </div>
    </div>

    <GenericToolbar :items="toolbarItems" />

    <div class="source-list">
      <button
        v-for="source in props.form.dataSources"
        :key="source.id"
        type="button"
        class="source-item"
        :class="{ active: selectedDataSource?.id === source.id }"
        @click="emit('select', source)"
      >
        <div class="source-name">
          {{ source.sourceName || '未設定' }}
        </div>

        <div class="source-meta">
          {{ source.tableName || 'table未設定' }}
        </div>

        <div class="source-flags">
          <v-chip
            size="x-small"
            variant="tonal"
            :color="source.activeFlag ? 'primary' : 'grey'"
          >
            {{ source.activeFlag ? 'active' : 'inactive' }}
          </v-chip>

          <v-chip
            size="x-small"
            variant="tonal"
            color="blue-grey"
          >
            {{ source.singleRowFlag ? 'single' : 'list' }}
          </v-chip>
        </div>
      </button>

      <div
        v-if="props.form.dataSources.length === 0"
        class="empty-list"
      >
        DataSourceを追加してください。
      </div>
    </div>
  </div>
</template>

<style scoped>
.source-list-card {
  display: grid;
  gap: 12px;
  align-content: start;
  height: 100%;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #f8fafc;
}

.card-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
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

.source-list {
  display: grid;
  gap: 8px;
}

.source-item {
  display: grid;
  gap: 4px;
  width: 100%;
  padding: 12px;
  text-align: left;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #ffffff;
  cursor: pointer;
}

.source-item:hover {
  background: #eff6ff;
}

.source-item.active {
  border-color: #2563eb;
  background: #dbeafe;
}

.source-name {
  font-weight: 700;
  color: #0f172a;
}

.source-meta {
  font-size: 12px;
  color: #64748b;
}

.source-flags {
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