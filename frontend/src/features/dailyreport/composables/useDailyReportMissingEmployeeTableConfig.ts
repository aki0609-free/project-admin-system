import { computed, type Ref } from 'vue'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { DailyReportMissingEmployeeResponse } from '@/features/dailyreport/types/dailyReportApiTypes'

export type DailyReportMissingEmployeeTableRow =
  SimpleTableEditableRow & {
    id: number
    employeeId: number
    employeeCode: string
    employeeName: string
    raw: DailyReportMissingEmployeeResponse
  }

export const useDailyReportMissingEmployeeTableConfig = (
  items: Ref<DailyReportMissingEmployeeResponse[]>,
) => {
  const rows = computed<DailyReportMissingEmployeeTableRow[]>(() =>
    items.value.map((item) => ({
      id: item.employeeId,
      employeeId: item.employeeId,
      employeeCode: item.employeeCode,
      employeeName: item.employeeName,
      raw: item,
    })),
  )

  const columns = computed(() => {
    const defs: SimpleTableColumnDef<DailyReportMissingEmployeeTableRow>[] = [
      {
        title: '社員ID',
        key: 'employeeId',
        width: '200px',
        filter: { type: 'text' },
      }, 
      {
        title: '社員コード',
        key: 'employeeCode',
        width: '200px',
        filter: { type: 'text' },
      },
      {
        title: '氏名',
        key: 'employeeName',
        width: '200px',
        filter: { type: 'text' },
      },
    ]

    return defs
  })

  const filterRules = computed(() =>
    createSimpleTableFilterRules<DailyReportMissingEmployeeTableRow>(
      columns.value,
    ),
  )

  return {
    rows,
    columns,
    filterRules,
  }
}