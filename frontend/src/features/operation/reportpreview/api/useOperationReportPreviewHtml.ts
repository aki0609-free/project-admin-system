import { ref, watch, type Ref } from 'vue'
import axios from 'axios'
import axiosApiClient from '@/app/plugins/axiosApiClient'

type ApiErrorResponse = {
  message?: string
  traceId?: string
}

const parseApiError = (value: unknown): ApiErrorResponse | null => {
  if (typeof value === 'object' && value !== null) {
    return value as ApiErrorResponse
  }
  if (typeof value !== 'string') return null

  try {
    return JSON.parse(value) as ApiErrorResponse
  } catch {
    return null
  }
}

export const resolvePreviewErrorMessage = (error: unknown): string => {
  if (!axios.isAxiosError(error)) {
    return '帳票プレビューの取得に失敗しました。'
  }

  const response = parseApiError(error.response?.data)
  const message = response?.message || '帳票プレビューの取得に失敗しました。'
  return response?.traceId
    ? `${message}（Trace ID: ${response.traceId}）`
    : message
}

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
    } catch (error) {
      if (sequence === requestSequence) {
        previewHtml.value = ''
        previewError.value = resolvePreviewErrorMessage(error)
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
