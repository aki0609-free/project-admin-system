import { ref, watch } from 'vue'
import { useApplicationMediasQuery } from '@/features/application/api/useApplicationMediasQuery'
import type { ApplicationMediaLocalItem } from '@/features/application/types/applicationMediaTypes'
import { useSaveApplicationMediaBulkMutation } from '@/features/application/api/useSaveApplicationMediaBulkMutation'
import { toApplicationMediaBulkSaveRequest } from '@/features/application/mapper/applicationMediaBulkSaveMapper'
import type { ApplicationMediaListItem } from '@/features/application/types/types'

function toLocalItem(item: ApplicationMediaListItem): ApplicationMediaLocalItem {
  return {
    ...item,
    isCreated: false,
    isUpdated: false,
    isDeleted: false,
  }
}

export const useApplicationMediaSource = () => {
  const mediasQuery = useApplicationMediasQuery()
  const saveBulkMutation = useSaveApplicationMediaBulkMutation()
  const medias = ref<ApplicationMediaLocalItem[]>([])
  const isDirty = ref(false)
  const saveMessage = ref('')
  const saveSuccess = ref(false)
  const showSaveAlert = ref(false)

  watch(
    () => mediasQuery.data.value,
    (rows) => {
      if (!rows) return

      // 未保存変更がある間はサーバー再取得で上書きしない
      if (isDirty.value) return

      medias.value = rows.map(toLocalItem)
    },
    { immediate: true },
  )

  const markDirty = () => {
    isDirty.value = true
  }

  const closeSaveAlert = () => {
    showSaveAlert.value = false
  }

  const saveChanges = async () => {
    const request = toApplicationMediaBulkSaveRequest(medias.value)

    if (
      request.created.length === 0 &&
      request.updated.length === 0 &&
      request.deletedIds.length === 0
    ) {
      isDirty.value = false
      return
    }

    try {
      await saveBulkMutation.mutateAsync(request)

      isDirty.value = false
      saveMessage.value = '保存しました。'
      saveSuccess.value = true
      showSaveAlert.value = true
    } catch (error) {
      saveMessage.value = '保存に失敗しました。'
      saveSuccess.value = false
      showSaveAlert.value = true
      throw error
    }
  }

  return {
    mediasQuery,
    medias,
    isDirty,
    saving: saveBulkMutation.isPending,
    markDirty,
    saveChanges,
    saveMessage,
    saveSuccess,
    showSaveAlert,
    closeSaveAlert,
  }
}
