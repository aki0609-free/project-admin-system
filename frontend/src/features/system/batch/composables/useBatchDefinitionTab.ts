import { computed, onBeforeUnmount, ref } from 'vue'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import { useBatchJobDefinitionsQuery } from '@/features/system/batch/api/queries/useBatchJobDefinitionsQuery'
import { useCreateBatchJobDefinitionMutation } from '@/features/system/batch/api/mutations/useCreateBatchJobDefinitionMutation'
import { useUpdateBatchJobDefinitionMutation } from '@/features/system/batch/api/mutations/useUpdateBatchJobDefinitionMutation'
import { useDeleteBatchJobDefinitionMutation } from '@/features/system/batch/api/mutations/useDeleteBatchJobDefinitionMutation'
import { useExecuteBatchMutation } from '@/features/system/batch/api/mutations/useExecuteBatchMutation'
import { useExecuteScheduledBatchMutation } from '@/features/system/batch/api/mutations/useExecuteScheduledBatchMutation'
import { useDownloadBatchLogFileMutation } from '@/features/system/batch/api/mutations/useDownloadBatchLogFileMutation'
import { useBatchDefinitionDialog } from '@/features/system/batch/composables/useBatchDefinitionDialog'
import type { BatchJobDefinitionForm } from '@/features/system/batch/types/batchFormTypes'
import type {
  BatchExecutePayload,
  BatchExecuteResponse,
  BatchJobDefinitionResponse,
} from '@/features/system/batch/types/batchApiTypes'
import { toBatchJobDefinitionSaveRequest } from '@/features/system/batch/utils/batchJobDefinitionConverters'

export const useBatchDefinitionTab = () => {
  const definitionsQuery = useBatchJobDefinitionsQuery()
  const createMutation = useCreateBatchJobDefinitionMutation()
  const updateMutation = useUpdateBatchJobDefinitionMutation()
  const deleteMutation = useDeleteBatchJobDefinitionMutation()
  const executeMutation = useExecuteBatchMutation()
  const executeScheduledMutation = useExecuteScheduledBatchMutation()
  const downloadFileMutation = useDownloadBatchLogFileMutation()

  const dialog = useBatchDefinitionDialog()

  const executeDialogVisible = ref(false)
  const executeTarget = ref<BatchJobDefinitionResponse | null>(null)

  const previewVisible = ref(false)
  const previewUrl = ref<string | null>(null)
  const previewTitle = ref('')

  const clearPreviewUrl = () => {
    if (!previewUrl.value) return

    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = null
  }

  onBeforeUnmount(() => {
    clearPreviewUrl()
  })

  const busy = computed(
    () =>
      definitionsQuery.isLoading.value ||
      dialog.detailQuery.isLoading.value ||
      createMutation.isPending.value ||
      updateMutation.isPending.value ||
      deleteMutation.isPending.value ||
      executeMutation.isPending.value ||
      executeScheduledMutation.isPending.value ||
      downloadFileMutation.isPending.value,
  )

  const save = async (form: BatchJobDefinitionForm) => {
    const request = toBatchJobDefinitionSaveRequest(form)

    if (form.id > 0) {
      await updateMutation.mutateAsync({
        id: form.id,
        request,
      })
    } else {
      await createMutation.mutateAsync(request)
    }

    dialog.close()
  }

  const remove = async (form: BatchJobDefinitionForm) => {
    if (form.id <= 0) {
      dialog.close()
      return
    }

    const ok = window.confirm(`「${form.jobName}」を削除しますか？`)
    if (!ok) return

    await deleteMutation.mutateAsync(form.id)
    dialog.close()
  }

  const toDefinitionResponse = (
    form: BatchJobDefinitionForm,
  ): BatchJobDefinitionResponse => ({
    id: form.id,
    jobCode: form.jobCode,
    jobName: form.jobName,
    jobType: form.jobType,
    targetCode: form.targetCode,
    immediateExecutable: form.immediateExecutable,
    scheduleEnabled: form.scheduleEnabled,
    scheduleType: form.scheduleType,
    cronExpression: form.cronExpression || null,
    lastExecutedAt: null,
    nextExecuteAt: null,
    activeFlag: form.activeFlag,
    description: form.description || null,
  })

  const openExecuteNow = (form: BatchJobDefinitionForm) => {
    if (!form.jobCode) return

    executeTarget.value =
      definitionsQuery.definitions.value.find((item) => item.id === form.id) ??
      toDefinitionResponse(form)

    executeDialogVisible.value = true
  }

  const openPreviewFromResult = async (
    result: BatchExecuteResponse,
  ) => {
    if (result.status !== 'COMPLETED') {
      alert(result.message)
      return
    }

    if (!result.logId || !result.outputFileName) {
      alert(result.message || 'バッチ実行が完了しました。')
      return
    }

    const blob = await downloadFileMutation.mutateAsync(result.logId) as Blob | MediaSource

    clearPreviewUrl()

    previewUrl.value = URL.createObjectURL(blob)
    previewTitle.value = result.outputFileName ?? result.jobCode
    previewVisible.value = true
  }

  const executeNow = async (payload: BatchExecutePayload) => {
    const result = await executeMutation.mutateAsync(payload) as BatchExecuteResponse

    executeDialogVisible.value = false

    await openPreviewFromResult(result)
  }

  const executeScheduled = async (form: BatchJobDefinitionForm) => {
    if (!form.jobCode) return

    const result = await executeScheduledMutation.mutateAsync(form.jobCode) as BatchExecuteResponse

    await openPreviewFromResult(result)
  }

  const toolbarItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: '新規作成',
      color: 'primary',
      disabled: busy.value,
      onClick: dialog.openCreate,
    },
  ])

  return {
    definitionsQuery,
    dialog,
    toolbarItems,

    save,
    remove,

    openExecuteNow,
    executeNow,
    executeScheduled,

    previewVisible,
    previewUrl,
    previewTitle,

    executeDialogVisible,
    executeTarget,
    executeLoading: executeMutation.isPending,
  }
}