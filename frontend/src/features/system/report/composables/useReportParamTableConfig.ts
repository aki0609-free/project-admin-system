import { computed, type Ref } from 'vue'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { ReportParamFormItem } from '@/features/system/report/types/reportFormTypes'
import {
  ReportParamControlType,
  ReportParamType,
} from '@/features/system/report/types/reportEnums'

export type ReportParamTableRow = SimpleTableEditableRow & {
  id: number
  paramName: string
  paramLabel: string
  paramType: ReportParamType
  controlType: ReportParamControlType
  requiredText: string
  activeText: string
  displayOrder: number
  raw: ReportParamFormItem
}

export const useReportParamTableConfig = (
  params: Ref<ReportParamFormItem[]>,
) => {
  const rows = computed<ReportParamTableRow[]>(() =>
    params.value.map((item) => ({
      id: item.id,
      paramName: item.paramName,
      paramLabel: item.paramLabel,
      paramType: item.paramType,
      controlType: item.controlType,
      requiredText: item.requiredFlag ? '必須' : '任意',
      activeText: item.activeFlag ? '有効' : '無効',
      displayOrder: item.displayOrder,
      raw: item,
    })),
  )

  const columns = computed(() => {
    const defs: SimpleTableColumnDef<ReportParamTableRow>[] = [
      { title: '順', key: 'displayOrder', width: '80px', filter: { type: 'text' } },
      { title: 'paramName', key: 'paramName', width: '180px', filter: { type: 'text' } },
      { title: '表示名', key: 'paramLabel', width: '180px', filter: { type: 'text' } },
      { title: '型', key: 'paramType', width: '140px', filter: { type: 'text' } },
      { title: 'UI', key: 'controlType', width: '140px', filter: { type: 'text' } },
      { title: '必須', key: 'requiredText', width: '100px', filter: { type: 'text' } },
      { title: '状態', key: 'activeText', width: '100px', filter: { type: 'text' } },
    ]

    return defs
  })

  const filterRules = computed(() =>
    createSimpleTableFilterRules<ReportParamTableRow>(columns.value),
  )

  return {
    rows,
    columns,
    filterRules,
  }
}