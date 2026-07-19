import { computed, type Ref } from 'vue'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { EmployeeSavingResponse } from '../types/employeeWorkApiTypes'

export type EmployeeSavingTableRow = SimpleTableEditableRow & {
  id: number
  employeeCode: string
  employeeName: string
  percentage: number
  minSalaryThreshold: number
  currentBalance: number
  activeText: string
  approvalStatus: string
  approvalComment: string
  raw: EmployeeSavingResponse
}

export const useEmployeeSavingTableConfig = (
  savings: Ref<EmployeeSavingResponse[]>,
) => {
  const rows = computed<EmployeeSavingTableRow[]>(() =>
    savings.value.map((item) => ({
      id: item.id,
      employeeCode: item.employeeCode,
      employeeName: item.employeeName,
      percentage: item.percentage,
      minSalaryThreshold: item.minSalaryThreshold,
      currentBalance: item.currentBalance,
      activeText: item.activeFlag ? '有効' : '無効',
      approvalStatus: item.approvalStatus,
      approvalComment: item.approvalComment ?? '',
      raw: item,
    })),
  )

  const columns = computed(() => {
    const defs: SimpleTableColumnDef<EmployeeSavingTableRow>[] = [
      { title: 'ID', key: 'id', width: '180px', filter: { type: 'text' } },
      { title: '社員コード', key: 'employeeCode', width: '180px', filter: { type: 'text' } },
      { title: '氏名', key: 'employeeName', width: '180px', filter: { type: 'text' } },
      { title: '貯蓄残高', key: 'currentBalance', width: '180px', filter: { type: 'text' } },
      { title: '貯蓄率%', key: 'percentage', width: '180px', filter: { type: 'text' } },
      { title: '最低給与額', key: 'minSalaryThreshold', width: '180px', filter: { type: 'text' } },
      { title: '状態', key: 'activeText', width: '100px', filter: { type: 'text' } },
      { title: '承認', key: 'approvalStatus', width: '180px', filter: { type: 'text' } },
      { title: 'コメント', key: 'approvalComment', width: '260px', filter: { type: 'text' } },
    ]

    return defs
  })

  const filterRules = computed(() =>
    createSimpleTableFilterRules<EmployeeSavingTableRow>(columns.value),
  )

  return {
    rows,
    columns,
    filterRules,
  }
}