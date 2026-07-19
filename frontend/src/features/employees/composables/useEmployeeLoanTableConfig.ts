import { computed, type Ref } from 'vue'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { EmployeeLoanResponse } from '../types/employeeWorkApiTypes'

export type EmployeeLoanTableRow = SimpleTableEditableRow & {
  id: number
  employeeCode: string
  employeeName: string
  principal: number
  currentBalance: number
  monthlyRepayment: number
  loanDate: string
  repaymentStartDate: string
  activeText: string
  approvalStatus: string
  approvalComment: string
  raw: EmployeeLoanResponse
}

export const useEmployeeLoanTableConfig = (
  loans: Ref<EmployeeLoanResponse[]>,
) => {
  const rows = computed<EmployeeLoanTableRow[]>(() =>
    loans.value.map((item) => ({
      id: item.id,
      employeeCode: item.employeeCode,
      employeeName: item.employeeName,
      principal: item.principal,
      currentBalance: item.currentBalance,
      monthlyRepayment: item.monthlyRepayment,
      loanDate: item.loanDate ?? '',
      repaymentStartDate: item.repaymentStartDate ?? '',
      activeText: item.activeFlag ? '有効' : '完了',
      approvalStatus: item.approvalStatus,
      approvalComment: item.approvalComment ?? '',
      raw: item,
    })),
  )

  const columns = computed(() => {
    const defs: SimpleTableColumnDef<EmployeeLoanTableRow>[] = [
      { title: 'ID', key: 'id', width: '180px', filter: { type: 'text' } },
      { title: '社員コード', key: 'employeeCode', width: '180px', filter: { type: 'text' } },
      { title: '氏名', key: 'employeeName', width: '180px', filter: { type: 'text' } },
      { title: '借入元本', key: 'principal', width: '180px', filter: { type: 'text' } },
      { title: '借入残高', key: 'currentBalance', width: '180px', filter: { type: 'text' } },
      { title: '月返済額', key: 'monthlyRepayment', width: '180px', filter: { type: 'text' } },
      { title: '借入日', key: 'loanDate', width: '180px', filter: { type: 'text' } },
      { title: '返済開始日', key: 'repaymentStartDate', width: '180px', filter: { type: 'text' } },
      { title: '状態', key: 'activeText', width: '180px', filter: { type: 'text' } },
      { title: '承認', key: 'approvalStatus', width: '180px', filter: { type: 'text' } },
      { title: 'コメント', key: 'approvalComment', width: '260px', filter: { type: 'text' } },
    ]

    return defs
  })

  const filterRules = computed(() =>
    createSimpleTableFilterRules<EmployeeLoanTableRow>(columns.value),
  )

  return {
    rows,
    columns,
    filterRules,
  }
}