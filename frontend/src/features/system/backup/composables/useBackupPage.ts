import { computed, ref, type Ref } from 'vue'

import type { ToolbarItem } from '@/shared/components/toolbar/types/types'

import { useBackupTargetsQuery } from '@/features/system/backup/api/useBackupTargetsQuery'
import { useBackupTargetDetailQuery } from '@/features/system/backup/api/useBackupTargetDetailQuery'
import { useCreateBackupTargetMutation } from '@/features/system/backup/api/useCreateBackupTargetMutation'
import { useUpdateBackupTargetMutation } from '@/features/system/backup/api/useUpdateBackupTargetMutation'
import { useDeleteBackupTargetMutation } from '@/features/system/backup/api/useDeleteBackupTargetMutation'
import { useExecuteBackupMutation } from '@/features/system/backup/api/useExecuteBackupMutation'

import { downloadBlob } from '@/features/system/backup/utils/downloadBlob'
import { toBackupTargetSaveRequest } from '@/features/system/backup/utils/backupTargetConverters'

import type { BackupTargetResponse } from '@/features/system/backup/types/backupApiTypes'
import type { BackupTargetDialogForm } from '@/features/system/backup/types/backupFormTypes'

type BackupPageTab = 'targets' | 'histories'

export const useBackupPage = (
  activeTab?: Ref<BackupPageTab>,
) => {
  const backupTargetsQuery = useBackupTargetsQuery()
  const createTargetMutation = useCreateBackupTargetMutation()
  const updateTargetMutation = useUpdateBackupTargetMutation()
  const deleteTargetMutation = useDeleteBackupTargetMutation()
  const executeBackupMutation = useExecuteBackupMutation()

  const selectedCodes = ref<string[]>([])
  const dialogVisible = ref(false)
  const editingTargetId = ref<number | null>(null)
  const isCreateMode = ref(false)

  const detailQuery = useBackupTargetDetailQuery(
    computed(() => editingTargetId.value),
  )

  const dialogTarget = computed(() => {
    if (isCreateMode.value) return null
    return detailQuery.target.value
  })

  const selectedCount = computed(() => selectedCodes.value.length)

  const isAllSelected = computed(() => {
    const rows = backupTargetsQuery.targets.value
    return rows.length > 0 && selectedCodes.value.length === rows.length
  })

  const busy = computed(
    () =>
      backupTargetsQuery.isLoading.value ||
      detailQuery.isLoading.value ||
      createTargetMutation.isPending.value ||
      updateTargetMutation.isPending.value ||
      deleteTargetMutation.isPending.value ||
      executeBackupMutation.isPending.value,
  )

  const openCreateDialog = () => {
    isCreateMode.value = true
    editingTargetId.value = null
    dialogVisible.value = true
  }

  const openEditDialog = (target: BackupTargetResponse) => {
    isCreateMode.value = false
    editingTargetId.value = target.id
    dialogVisible.value = true
  }

  const closeDialog = () => {
    dialogVisible.value = false
    editingTargetId.value = null
    isCreateMode.value = false
  }

  const toggleAll = () => {
    const rows = backupTargetsQuery.targets.value

    selectedCodes.value =
      isAllSelected.value
        ? []
        : rows.map(row => row.targetCode)
  }

  const executeBackup = async () => {
    if (selectedCodes.value.length === 0) {
      alert('バックアップ対象を1件以上選択してください。')
      return
    }

    try {
      const blob = await executeBackupMutation.mutateAsync({
        targetCodes: selectedCodes.value,
      }) as Blob

      const timestamp = new Date()
        .toISOString()
        .slice(0, 19)
        .replace(/[-:T]/g, '')

      const fileName =
        selectedCodes.value.length === 1
          ? `${selectedCodes.value[0]}_${timestamp}.csv`
          : `backup_${timestamp}.zip`

      downloadBlob(blob, fileName)

      selectedCodes.value = []

      if (activeTab) {
        activeTab.value = 'histories'
      }
    } catch (error) {
      console.error(error)
      alert('バックアップに失敗しました。')
    }
  }

  const onSaveTarget = async (form: BackupTargetDialogForm) => {
    const request = toBackupTargetSaveRequest(form)

    if (form.id > 0) {
      await updateTargetMutation.mutateAsync({
        id: form.id,
        request,
      })
    } else {
      await createTargetMutation.mutateAsync(request)
    }

    closeDialog()
  }

  const onDeleteTarget = async (form: BackupTargetDialogForm) => {
    if (form.id <= 0) {
      closeDialog()
      return
    }

    const ok = window.confirm(`「${form.targetCode}」を削除しますか？`)
    if (!ok) return

    await deleteTargetMutation.mutateAsync({
      id: form.id,
    })

    closeDialog()
  }

  const toolbarItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: '新規追加',
      color: 'primary',
      disabled: busy.value,
      onClick: openCreateDialog,
    },
    {
      type: 'button',
      label: isAllSelected.value ? '全解除' : '全選択',
      color: 'secondary',
      disabled: busy.value || backupTargetsQuery.targets.value.length === 0,
      onClick: toggleAll,
    },
    {
      type: 'button',
      label: executeBackupMutation.isPending.value
        ? '出力中...'
        : `選択した ${selectedCount.value} 件を出力`,
      color: 'success',
      disabled: busy.value || selectedCount.value === 0,
      loading: executeBackupMutation.isPending.value,
      onClick: executeBackup,
    },
  ])

  return {
    backupTargetsQuery,
    selectedCodes,

    dialogVisible,
    dialogTarget,

    selectedCount,
    toolbarItems,

    openEditDialog,
    onSaveTarget,
    onDeleteTarget,
  }
}