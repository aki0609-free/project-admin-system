import { computed, ref, watch, type Ref } from 'vue'
import type { ReportParamFormItem } from '@/features/system/report/types/reportFormTypes'
import type { ReportParamTableRow } from '@/features/system/report/composables/useReportParamTableConfig'

export const useReportParamSelection = (
  params: Ref<ReportParamFormItem[]>,
) => {
  const selectedParamId = ref<number | null>(null)

  watch(
    params,
    (items) => {
      if (!items.length) {
        selectedParamId.value = null
        return
      }

      const exists = items.some((item) => item.id === selectedParamId.value)
      if (!exists) {
        selectedParamId.value = items[0]?.id ?? null
      }
    },
    { immediate: true, deep: true },
  )

  const selectedParamIndex = computed(() => {
    if (selectedParamId.value == null) return -1
    return params.value.findIndex((item) => item.id === selectedParamId.value)
  })

  const selectedParamModel = computed<ReportParamFormItem | null>({
    get: () => {
      const index = selectedParamIndex.value
      if (index < 0) return null
      return params.value[index] ?? null
    },
    set: (value) => {
      const index = selectedParamIndex.value
      if (index < 0 || value == null) return

      const nextParams = [...params.value]
      nextParams[index] = value
      params.value = nextParams
    },
  })

  const hasSelection = computed(() => selectedParamModel.value != null)

  const selectRow = (row: ReportParamTableRow) => {
    selectedParamId.value = row.id
  }

  return {
    selectedParamId,
    selectedParamIndex,
    selectedParamModel,
    hasSelection,
    selectRow,
  }
}