<script setup lang="ts">
import ImportExecuteForm from '@/features/system/import/components/ImportExecuteForm.vue'
import { useImportExecuteTab } from '@/features/system/import/composables/useImportExecuteTab'

const {
  targetsQuery,
  pending,
  lastResultMessage,
  lastJobExecutionId,
  executeImport,
} = useImportExecuteTab()
</script>

<template>
  <div class="import-execute-tab">
    <div class="tab-header">
      <h3>インポート実行</h3>
      <p class="tab-description">
        取込定義を選択して、CSV取込を実行します。
      </p>
    </div>

    <ImportExecuteForm
      :targets="targetsQuery.targets.value"
      :pending="pending"
      @execute="executeImport"
    />

    <v-alert
      v-if="lastResultMessage"
      type="success"
      variant="tonal"
      border="start"
    >
      <div class="result-content">
        <div>
          <strong>結果：</strong>{{ lastResultMessage }}
        </div>
        <div v-if="lastJobExecutionId">
          <strong>JobExecutionId：</strong>{{ lastJobExecutionId }}
        </div>
      </div>
    </v-alert>
  </div>
</template>

<style scoped>
.import-execute-tab {
  display: grid;
  gap: 16px;
}

.tab-header {
  display: grid;
  gap: 6px;
}

.tab-description {
  margin: 0;
  color: #64748b;
}

.result-content {
  display: grid;
  gap: 6px;
}
</style>