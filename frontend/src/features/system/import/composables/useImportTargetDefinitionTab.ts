import { computed } from 'vue'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import { useImportTargetsQuery } from '@/features/system/import/api/useImportTargetsQuery'
import { useCreateImportTargetMutation } from '@/features/system/import/api/useCreateImportTargetMutation'
import { useUpdateImportTargetMutation } from '@/features/system/import/api/useUpdateImportTargetMutation'
import { useDeleteImportTargetMutation } from '@/features/system/import/api/useDeleteImportTargetMutation'
import { useImportTargetDialog } from '@/features/system/import/composables/useImportTargetDialog'
import { toImportTargetSaveRequest } from '@/features/system/import/utils/importTargetConverters'
import type { ImportTargetDialogForm } from '@/features/system/import/types/importFormTypes'

export const useImportTargetDefinitionTab = () => {
  const targetsQuery = useImportTargetsQuery(false)
  const createMutation = useCreateImportTargetMutation()
  const updateMutation = useUpdateImportTargetMutation()
  const deleteMutation = useDeleteImportTargetMutation()

  const dialog = useImportTargetDialog()

  const busy = computed(
    () =>
      targetsQuery.isLoading.value ||
      dialog.detailQuery.isLoading.value ||
      createMutation.isPending.value ||
      updateMutation.isPending.value ||
      deleteMutation.isPending.value,
  )

  const onSave = async (form: ImportTargetDialogForm) => {
    const request = toImportTargetSaveRequest(form)

    if (form.id > 0) {
      await updateMutation.mutateAsync({
        id: form.id,
        request,
      })
    } else {
      await createMutation.mutateAsync(request)
    }

    dialog.closeDialog()
  }

  const onDelete = async (form: ImportTargetDialogForm) => {
    if (form.id <= 0) {
      dialog.closeDialog()
      return
    }

    const ok = window.confirm(`「${form.targetCode}」を削除しますか？`)
    if (!ok) return

    await deleteMutation.mutateAsync({
      id: form.id,
    })

    dialog.closeDialog()
  }

  const toolbarItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: '新規追加',
      color: 'primary',
      disabled: busy.value,
      onClick: dialog.openCreateDialog,
    },
  ])

  return {
    targetsQuery,
    dialog,
    busy,
    toolbarItems,
    onSave,
    onDelete,
  }
}