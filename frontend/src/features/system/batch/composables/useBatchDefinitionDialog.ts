import { computed, ref } from 'vue'
import { useBatchJobDefinitionDetailQuery } from '@/features/system/batch/api/queries/useBatchJobDefinitionDetailQuery'
import type { BatchDefinitionTableRow } from '@/features/system/batch/composables/useBatchDefinitionTableConfig'

export const useBatchDefinitionDialog = () => {
  const visible = ref(false)
  const editingId = ref<number | null>(null)

  const detailQuery = useBatchJobDefinitionDetailQuery(
    computed(() => editingId.value),
  )

  const dialogDefinition = computed(() =>
    editingId.value == null ? null : detailQuery.definition.value,
  )

  const openCreate = () => {
    editingId.value = null
    visible.value = true
  }

  const openEdit = (row: BatchDefinitionTableRow) => {
    editingId.value = row.id
    visible.value = true
  }

  const close = () => {
    visible.value = false
  }

  return {
    visible,
    editingId,
    detailQuery,
    dialogDefinition,
    openCreate,
    openEdit,
    close,
  }
}