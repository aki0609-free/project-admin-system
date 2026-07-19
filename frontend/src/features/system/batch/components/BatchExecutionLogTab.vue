<script setup lang="ts">
import BatchExecutionLogTable from '@/features/system/batch/components/BatchExecutionLogTable.vue'
import { useBatchExecutionLogsQuery } from '@/features/system/batch/api/queries/useBatchExecutionLogsQuery'

const logsQuery = useBatchExecutionLogsQuery()
</script>

<template>
  <div class="batch-tab">
    <div class="tab-header">
      <h3>実行履歴</h3>
      <p class="tab-description">
        バッチ実行結果とエラー内容を確認します。
      </p>
    </div>

    <BatchExecutionLogTable :items="logsQuery.logs.value" />
  </div>
</template>

<style scoped>
.batch-tab {
  display: grid;
  gap: 12px;
}

.tab-header {
  display: grid;
  gap: 6px;
}

.tab-description {
  margin: 0;
  color: #64748b;
}
</style>