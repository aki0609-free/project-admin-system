import { computed, type Ref } from 'vue'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import { formatYearMonthDay } from '@/shared/utils/DateUtils'
import type {
  EmployeeFinanceTransactionResponse,
  EmployeeFinanceTransactionType,
} from '../types/employeeWorkApiTypes'

const transactionTypeLabels: Record<EmployeeFinanceTransactionType, string> = {
  OPENING_BALANCE: '初期残高',
  LOAN_DISBURSEMENT: '貸付登録',
  LOAN_DISBURSEMENT_REVERSAL: '貸付登録取消',
  LOAN_PRINCIPAL_ADJUSTMENT: '借入元本訂正',
  LOAN_REPAYMENT: '貸付返済',
  LOAN_REPAYMENT_REVERSAL: '貸付返済取消',
  SAVING_DEPOSIT: '積立',
  SAVING_DEPOSIT_REVERSAL: '積立取消',
}

export type EmployeeFinanceTransactionTableRow = SimpleTableEditableRow & {
  id: number
  transactionDate: string
  employeeCode: string
  employeeName: string
  accountTypeText: string
  transactionTypeText: string
  amount: number
  balanceBefore: number
  balanceAfter: number
  dailyReportId: string
  note: string
}

export const useEmployeeFinanceTransactionTableConfig = (
  transactions: Ref<EmployeeFinanceTransactionResponse[]>,
) => {
  const rows = computed<EmployeeFinanceTransactionTableRow[]>(() =>
    transactions.value.map((item) => ({
      id: item.id,
      transactionDate: item.transactionDate,
      employeeCode: item.employeeCode,
      employeeName: item.employeeName,
      accountTypeText: item.accountType === 'LOAN' ? '貸付' : '貯蓄',
      transactionTypeText: transactionTypeLabels[item.transactionType],
      amount: item.amount,
      balanceBefore: item.balanceBefore,
      balanceAfter: item.balanceAfter,
      dailyReportId: item.dailyReportId == null ? '' : String(item.dailyReportId),
      note: item.note ?? '',
    })),
  )

  const columns = computed(() => {
    const defs: SimpleTableColumnDef<EmployeeFinanceTransactionTableRow>[] = [
      {
        title: '取引日',
        key: 'transactionDate',
        width: '160px',
        filter: { type: 'date' },
        formatter: value => formatYearMonthDay(String(value ?? '')),
      },
      { title: '社員コード', key: 'employeeCode', width: '150px', filter: { type: 'text' } },
      { title: '氏名', key: 'employeeName', width: '180px', filter: { type: 'text' } },
      { title: '区分', key: 'accountTypeText', width: '100px', filter: { type: 'text' } },
      { title: '取引種別', key: 'transactionTypeText', width: '180px', filter: { type: 'text' } },
      { title: '増減額', key: 'amount', width: '140px', filter: { type: 'text' } },
      { title: '変更前残高', key: 'balanceBefore', width: '160px', filter: { type: 'text' } },
      { title: '変更後残高', key: 'balanceAfter', width: '160px', filter: { type: 'text' } },
      { title: '日報ID', key: 'dailyReportId', width: '120px', filter: { type: 'text' } },
      { title: '備考', key: 'note', width: '240px', filter: { type: 'text' } },
    ]
    return defs
  })

  const filterRules = computed(() =>
    createSimpleTableFilterRules<EmployeeFinanceTransactionTableRow>(columns.value),
  )

  return { rows, columns, filterRules }
}
