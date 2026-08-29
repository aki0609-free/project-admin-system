<script setup lang="ts">
import { computed } from 'vue'

import { useExecuteBatchMutation } from '@/features/system/batch/api/mutations/useExecuteBatchMutation'
import { useDownloadBatchLogFileMutation } from '@/features/system/batch/api/mutations/useDownloadBatchLogFileMutation'
import type { BatchExecuteResponse } from '@/features/system/batch/types/batchApiTypes'
import { downloadBlob } from '@/features/system/backup/utils/downloadBlob'
import BatchParameterDialog from '@/shared/ui/dialog/BatchParameterDialog.vue'

import type { ToolbarItem } from './types'

type BatchToolbarItem = Extract<ToolbarItem, { type: 'batch' }>

const props = defineProps<{
  modelValue: boolean
  item: BatchToolbarItem | null
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
}>()

const dialogModel = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})

const executeBatchMutation = useExecuteBatchMutation()
const downloadBatchLogFileMutation = useDownloadBatchLogFileMutation()

const loading = computed(
  () => executeBatchMutation.isPending.value
    || downloadBatchLogFileMutation.isPending.value,
)

const executeBatch = async (payload: {
  jobCode: string
  params: Record<string, unknown>
}) => {
  const item = props.item
  const outputAction = item?.outputAction ?? 'none'
  const previewWindow = outputAction === 'preview'
    ? window.open('about:blank', '_blank')
    : null

  if (previewWindow) {
    previewWindow.opener = null
    previewWindow.document.title = '帳票を生成しています'
    previewWindow.document.body.textContent =
      '帳票を生成しています。しばらくお待ちください。'
  }

  try {
    const result = await executeBatchMutation.mutateAsync(payload) as BatchExecuteResponse
    dialogModel.value = false

    if (result.status !== 'COMPLETED') {
      previewWindow?.close()
      window.alert(result.message)
      return
    }

    if (outputAction !== 'none' && result.logId) {
      const blob = await downloadBatchLogFileMutation.mutateAsync(result.logId) as Blob
      const fileName = result.outputFileName ?? `batch-output-${result.logId}`

      if (outputAction === 'preview') {
        const url = window.URL.createObjectURL(blob)
        if (previewWindow) {
          previewWindow.location.replace(url)
        } else {
          window.open(url, '_blank', 'noopener,noreferrer')
        }
        window.setTimeout(
          () => window.URL.revokeObjectURL(url),
          5 * 60 * 1000,
        )
      } else {
        downloadBlob(blob, fileName)
      }
    } else {
      previewWindow?.close()
    }

    if (item?.onSuccess) {
      item.onSuccess(result.message)
    } else {
      window.alert(result.message)
    }
  } catch (error) {
    previewWindow?.close()
    throw error
  }
}
</script>

<template>
  <BatchParameterDialog
    v-model="dialogModel"
    :item="item"
    :loading="loading"
    @execute="executeBatch"
  />
</template>
