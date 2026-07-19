import { computed, type Ref } from 'vue'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type {
  DailyReportMonthlyAttendanceResponse,
  SalaryType,
} from '@/features/dailyreport/types/dailyReportApiTypes'
import { formatHours } from '@/shared/utils/TimeUtils'

const salaryTypeLabelMap: Record<SalaryType, string> = {
  MONTHLY: '月給',
  DAILY: '日給',
  HOURLY: '時給',
}

const formatSalaryType = (value: SalaryType | null): string =>
  value ? salaryTypeLabelMap[value] : ''

export type DailyReportMonthlyAttendanceTableRow = SimpleTableEditableRow & {
  id: number
  employeeId: number
  employeeCode: string
  employeeName: string

  salaryType: SalaryType | null
  salaryTypeText: string
  baseSalaryAmount: number
  grossSalaryAmount: number

  reportCount: number
  paidLeaveCount: number
  totalWorkHours: number
  totalOvertimeHours: number
  totalNightWorkHours: number

  totalAllowanceAmount: number
  totalDeductionAmount: number
  totalSavingAmount: number
  totalLoanRepaymentAmount: number
  estimatedPaymentAmount: number

  raw: DailyReportMonthlyAttendanceResponse
}

export const useDailyReportMonthlyAttendanceTableConfig = (
  items: Ref<DailyReportMonthlyAttendanceResponse[]>,
) => {
  const rows = computed<DailyReportMonthlyAttendanceTableRow[]>(() =>
    items.value.map((item) => ({
      id: item.employeeId,
      employeeId: item.employeeId,
      employeeCode: item.employeeCode,
      employeeName: item.employeeName,

      salaryType: item.salaryType,
      salaryTypeText: formatSalaryType(item.salaryType),
      baseSalaryAmount: item.baseSalaryAmount,
      grossSalaryAmount: item.grossSalaryAmount,

      reportCount: item.reportCount,
      paidLeaveCount: item.paidLeaveCount,

      totalWorkHours: item.totalWorkHours,
      totalOvertimeHours: item.totalOvertimeHours,
      totalNightWorkHours: item.totalNightWorkHours,

      totalAllowanceAmount: item.totalAllowanceAmount,
      totalDeductionAmount: item.totalDeductionAmount,
      totalSavingAmount: item.totalSavingAmount,
      totalLoanRepaymentAmount: item.totalLoanRepaymentAmount,
      estimatedPaymentAmount: item.estimatedPaymentAmount,

      raw: item,
    })),
  )

  const columns = computed(() => {
    const defs: SimpleTableColumnDef<DailyReportMonthlyAttendanceTableRow>[] = [
      { title: '社員コード', key: 'employeeCode', width: '180px', filter: { type: 'text' } },
      { title: '氏名', key: 'employeeName', width: '180px', filter: { type: 'text' } },

      { title: '支払区分', key: 'salaryTypeText', width: '140px', filter: { type: 'text' } },
      { title: '基本単価', key: 'baseSalaryAmount', width: '160px', filter: { type: 'text' } },
      { title: '支給見込', key: 'grossSalaryAmount', width: '160px', filter: { type: 'text' } },

      { title: '日報数', key: 'reportCount', width: '120px', filter: { type: 'text' } },
      { title: '有給日数', key: 'paidLeaveCount', width: '120px', filter: { type: 'text' } },

      {
        title: '通常時間',
        key: 'totalWorkHours',
        width: '140px',
        filter: { type: 'text' },
        formatter: (value) => formatHours(value as string),
      },
      {
        title: '残業時間',
        key: 'totalOvertimeHours',
        width: '140px',
        filter: { type: 'text' },
        formatter: (value) => formatHours(value as string),
      },
      {
        title: '深夜時間',
        key: 'totalNightWorkHours',
        width: '140px',
        filter: { type: 'text' },
        formatter: (value) => formatHours(value as string),
      },

      { title: '手当合計', key: 'totalAllowanceAmount', width: '160px', filter: { type: 'text' } },
      { title: '控除合計', key: 'totalDeductionAmount', width: '160px', filter: { type: 'text' } },
      { title: '貯蓄合計', key: 'totalSavingAmount', width: '160px', filter: { type: 'text' } },
      { title: '返済合計', key: 'totalLoanRepaymentAmount', width: '160px', filter: { type: 'text' } },
      { title: '差引見込', key: 'estimatedPaymentAmount', width: '160px', filter: { type: 'text' } },
    ]

    return defs
  })

  const filterRules = computed(() =>
    createSimpleTableFilterRules<DailyReportMonthlyAttendanceTableRow>(
      columns.value,
    ),
  )

  return {
    rows,
    columns,
    filterRules,
  }
}