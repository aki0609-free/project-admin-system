<script setup lang="ts">
import { computed, ref } from 'vue'
import type { PropType } from 'vue'
import { useExecuteBatchMutation } from '@/features/system/batch/api/mutations/useExecuteBatchMutation'
import { useDownloadBatchLogFileMutation } from '@/features/system/batch/api/mutations/useDownloadBatchLogFileMutation'
import type { BatchExecuteResponse } from '@/features/system/batch/types/batchApiTypes'
import { downloadBlob } from '@/features/system/backup/utils/downloadBlob'
import BatchParameterDialog from '@/shared/ui/dialog/BatchParameterDialog.vue'
import AppToolbar from './AppToolbar.vue'
import type { ToolbarItem } from './types'

const props = defineProps({
  leftItems: {
    type: Array as PropType<ToolbarItem[]>,
    default: () => [],
  },
  rightItems: {
    type: Array as PropType<ToolbarItem[]>,
    default: () => [],
  },
})

const executeBatchMutation = useExecuteBatchMutation()
const downloadBatchLogFileMutation = useDownloadBatchLogFileMutation()

const batchDialogVisible = ref(false)
const selectedBatchItem = ref<Extract<ToolbarItem, { type: 'batch' }> | null>(null)

const batchLoading = computed(
  () => executeBatchMutation.isPending.value || downloadBatchLogFileMutation.isPending.value,
)

const openBatchDialog = (item: Extract<ToolbarItem, { type: 'batch' }>) => {
  selectedBatchItem.value = item
  batchDialogVisible.value = true
}

const executeBatch = async (payload: { jobCode: string; params: Record<string, unknown> }) => {
  const item = selectedBatchItem.value
  const outputAction = item?.outputAction ?? 'none'
  const previewWindow = outputAction === 'preview' ? window.open('about:blank', '_blank') : null

  if (previewWindow) {
    previewWindow.opener = null
    previewWindow.document.title = '帳票を生成しています'
    previewWindow.document.body.textContent = '帳票を生成しています。しばらくお待ちください。'
  }

  try {
    const result = (await executeBatchMutation.mutateAsync(payload)) as BatchExecuteResponse
    batchDialogVisible.value = false

    if (result.status !== 'COMPLETED') {
      previewWindow?.close()
      alert(result.message)
      return
    }

    if (outputAction !== 'none' && result.logId) {
      const blob = (await downloadBatchLogFileMutation.mutateAsync(result.logId)) as Blob
      const fileName = result.outputFileName ?? `batch-output-${result.logId}`

      if (outputAction === 'preview') {
        const url = window.URL.createObjectURL(blob)
        if (previewWindow) {
          previewWindow.location.replace(url)
        } else {
          window.open(url, '_blank', 'noopener,noreferrer')
        }
        window.setTimeout(() => window.URL.revokeObjectURL(url), 5 * 60 * 1000)
      } else {
        downloadBlob(blob, fileName)
      }
    } else {
      previewWindow?.close()
    }

    if (item?.onSuccess) {
      item.onSuccess(result.message)
    } else {
      alert(result.message)
    }
  } catch (error) {
    previewWindow?.close()
    throw error
  }
}
</script>

<template>
  <AppToolbar
    :left-items="props.leftItems"
    :right-items="props.rightItems"
    @batch-click="openBatchDialog"
  />

  <BatchParameterDialog
    v-model="batchDialogVisible"
    :item="selectedBatchItem"
    :loading="batchLoading"
    @execute="executeBatch"
  />
</template>
