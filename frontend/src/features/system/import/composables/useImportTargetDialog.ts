import { computed, ref } from 'vue'
import { useImportTargetDetailQuery } from '@/features/system/import/api/useImportTargetDetailQuery'
import type { ImportTargetResponse } from '@/features/system/import/types/importApiTypes'

export const useImportTargetDialog = () => {
  const visible = ref(false)
  const editingTargetId = ref<number | null>(null)
  const isCreateMode = ref(false)

  const detailQuery = useImportTargetDetailQuery(
    computed(() => editingTargetId.value),
  )

  const dialogTarget = computed(() => {
    if (isCreateMode.value) return null
    return detailQuery.target.value
  })

  const openCreateDialog = () => {
    isCreateMode.value = true
    editingTargetId.value = null
    visible.value = true
  }

  const openEditDialog = (target: ImportTargetResponse) => {
    isCreateMode.value = false
    editingTargetId.value = target.id
    visible.value = true
  }

  const closeDialog = () => {
    visible.value = false
  }

  return {
    visible,
    editingTargetId,
    isCreateMode,
    dialogTarget,
    detailQuery,
    openCreateDialog,
    openEditDialog,
    closeDialog,
  }
}