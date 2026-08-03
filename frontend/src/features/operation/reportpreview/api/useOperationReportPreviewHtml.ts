import { ref, watch, type Ref } from 'vue'
import axiosApiClient from '@/app/plugins/axiosApiClient'

export const useOperationReportPreviewHtml = (
  previewUrl: Ref<string>,
) => {
  const previewHtml = ref('')
  const isPreviewLoading = ref(false)
  const previewError = ref<string | null>(null)
  let requestSequence = 0

  const loadPreview = async () => {
    const url = previewUrl.value
    const sequence = ++requestSequence

    if (!url) {
      previewHtml.value = ''
      previewError.value = null
      isPreviewLoading.value = false
      return
    }

    isPreviewLoading.value = true
    previewError.value = null

    try {
      const response = await axiosApiClient.get<string>(url, {
        responseType: 'text',
      })

      if (sequence === requestSequence) {
        previewHtml.value = response.data
      }
    } catch {
      if (sequence === requestSequence) {
        previewHtml.value = ''
        previewError.value = '帳票プレビューの取得に失敗しました。'
      }
    } finally {
      if (sequence === requestSequence) {
        isPreviewLoading.value = false
      }
    }
  }

  watch(previewUrl, loadPreview, { immediate: true })

  return {
    previewHtml,
    isPreviewLoading,
    previewError,
    reloadPreview: loadPreview,
  }
}
