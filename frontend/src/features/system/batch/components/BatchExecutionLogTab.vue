<script setup lang="ts">
import BatchExecutionLogTable from '@/features/system/batch/components/BatchExecutionLogTable.vue'
import { useBatchExecutionLogsQuery } from '@/features/system/batch/api/queries/useBatchExecutionLogsQuery'
import { useRetryBatchMutation } from '@/features/system/batch/api/mutations/useRetryBatchMutation'
import { useDownloadBatchLogFileMutation } from '@/features/system/batch/api/mutations/useDownloadBatchLogFileMutation'
import type { BatchExecutionLogResponse } from '@/features/system/batch/types/batchApiTypes'
import type { BatchExecuteResponse } from '@/features/system/batch/types/batchApiTypes'

const logsQuery = useBatchExecutionLogsQuery()
const retryMutation = useRetryBatchMutation()
const downloadMutation = useDownloadBatchLogFileMutation()

const retry = async (item: BatchExecutionLogResponse) => {
  const ok = window.confirm(`失敗した「${item.jobName}」を再実行しますか？`)
  if (!ok) return

  const result = await retryMutation.mutateAsync(item.id) as BatchExecuteResponse
  alert(result.message)
}

const download = async (item: BatchExecutionLogResponse) => {
  const blob = await downloadMutation.mutateAsync(item.id) as Blob
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = item.outputFileName || `batch-${item.id}`
  anchor.click()
  URL.revokeObjectURL(url)
}
</script>

<template>
  <div class="batch-tab">
    <div class="tab-header">
      <h3>実行履歴</h3>
      <p class="tab-description">
        バッチ実行結果とエラー内容を確認します。
      </p>
    </div>

    <BatchExecutionLogTable
      :items="logsQuery.logs.value"
      :busy="retryMutation.isPending.value || downloadMutation.isPending.value"
      @retry="retry"
      @download="download"
    />
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
