import { computed, type Ref } from 'vue'
import type {
  ReportMasterForm,
  ReportParamFormItem,
} from '@/features/system/report/types/reportFormTypes'
import { useReportParamSelection } from '@/features/system/report/composables/useReportParamSelection'
import { useReportParamTableConfig } from '@/features/system/report/composables/useReportParamTableConfig'
import { useReportParamDetailFormConfig } from '@/features/system/report/composables/useReportParamDetailFormConfig'
import {
  createEmptyReportParamFormItem,
  resequenceReportParams,
} from '@/features/system/report/utils/reportParamFormFactory'

export const useReportMasterParamsTab = (
  model: Ref<ReportMasterForm>,
) => {
  const params = computed<ReportParamFormItem[]>({
    get: () => model.value.params,
    set: (value) => {
      model.value = {
        ...model.value,
        params: value,
      }
    },
  })

  const {
    selectedParamId,
    selectedParamIndex,
    selectedParamModel,
    hasSelection,
    selectRow,
  } = useReportParamSelection(params)

  const {
    rows,
    columns,
    filterRules,
  } = useReportParamTableConfig(params)

  const {
    schema,
    sections,
  } = useReportParamDetailFormConfig()

  const add = () => {
    const next =
      createEmptyReportParamFormItem(params.value.length + 1)

    params.value =
      resequenceReportParams([
        ...params.value,
        next,
      ])

    selectedParamId.value = next.id
  }

  const remove = () => {
    const index = selectedParamIndex.value

    if (index < 0) return

    const nextParams = [...params.value]
    nextParams.splice(index, 1)

    params.value =
      resequenceReportParams(nextParams)

    selectedParamId.value = null
  }

  const moveUp = () => {
    const index = selectedParamIndex.value

    if (index <= 0) return

    const list = [...params.value]
    const current = list[index]
    const prev = list[index - 1]

    if (!current || !prev) return

    list[index - 1] = current
    list[index] = prev

    params.value =
      resequenceReportParams(list)

    selectedParamId.value = current.id
  }

  const moveDown = () => {
    const index = selectedParamIndex.value
    const list = [...params.value]

    if (index < 0 || index >= list.length - 1) return

    const current = list[index]
    const next = list[index + 1]

    if (!current || !next) return

    list[index] = next
    list[index + 1] = current

    params.value =
      resequenceReportParams(list)

    selectedParamId.value = current.id
  }

  return {
    rows,
    columns,
    filterRules,
    selectedParamModel,
    schema,
    sections,
    hasSelection,
    selectRow,
    add,
    remove,
    moveUp,
    moveDown,
  }
}