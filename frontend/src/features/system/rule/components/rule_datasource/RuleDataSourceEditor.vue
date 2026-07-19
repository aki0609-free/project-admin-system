<script setup lang="ts">
import DslEditor from '@/shared/components/editor/DslEditor.vue'
import type { RuleDataSourceForm } from '@/features/system/rule/types/ruleFormTypes'

defineProps<{
  dataSource: RuleDataSourceForm
}>()
</script>

<template>
  <div class="source-editor-card">
    <template v-if="dataSource">
      <div class="card-title">
        DataSource設定
      </div>

      <div class="source-editor-grid">
        <v-text-field
          v-model="dataSource.sourceName as string"
          label="sourceName"
          variant="outlined"
          density="comfortable"
        />

        <v-text-field
          v-model="dataSource.tableName as string"
          label="tableName"
          variant="outlined"
          density="comfortable"
        />

        <v-text-field
          v-model.number="dataSource.orderNo as number"
          label="orderNo"
          type="number"
          variant="outlined"
          density="comfortable"
        />

        <v-checkbox
          v-model="dataSource.singleRowFlag as boolean"
          label="singleRow"
          density="compact"
          hide-details
        />

        <v-checkbox
          v-model="dataSource.activeFlag as boolean"
          label="active"
          density="compact"
          hide-details
        />
      </div>

      <DslEditor
        v-model="dataSource.whereClause as string"
        label="whereClause"
        hint="WHERE は不要です。例：employee_id = :employeeId"
        :rows="5"
      />
    </template>

    <div
      v-else
      class="empty-card"
    >
      左の一覧からDataSourceを選択してください。
    </div>
  </div>
</template>

<style scoped>
.source-editor-card {
  display: grid;
  gap: 14px;
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

.source-editor-grid {
  display: grid;
  grid-template-columns: 1.1fr 1.1fr 120px 120px 100px;
  gap: 12px;
  align-items: center;
}

.empty-card {
  min-height: 220px;
  display: grid;
  place-items: center;
  color: #64748b;
}

@media (max-width: 1280px) {
  .source-editor-grid {
    grid-template-columns: 1fr;
  }
}
</style>