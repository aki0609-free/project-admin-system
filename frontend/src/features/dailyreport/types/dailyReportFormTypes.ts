import type {
  ApprovalStatus,
  CustomerBillingUnit,
} from '@/features/dailyreport/types/dailyReportApiTypes'

import type {
  DailyReportAmountItemForm,
} from './dailyReportInputItemTypes'

export type DailyReportForm = {
  id: number

  employeeId: number | null

  workDate: string
  paymentDate: string

  customerId: number | null
  customerSiteId: number | null

  customerName: string
  siteName: string

  /**
   * 画面プレビュー用。
   * 保存時の正式な適用単価はサーバー側で再解決する。
   */
  billingRateId: number | null

  jobCode: string
  jobName: string

  siteRoleCode: string
  siteRoleName: string

  billingUnit: CustomerBillingUnit | null

  billingBaseUnitPrice: number
  billingOvertimeUnitPrice: number
  billingNightUnitPrice: number
  billingHolidayUnitPrice: number
  billingCommuteUnitPrice: number

  workDescription: string

  startTime: string
  endTime: string

  breakMinutes: number

  /**
   * 通常労働時間。
   * 休日労働時間は含めない。
   */
  workHours: number

  /**
   * 早出・残業時間。
   */
  overtimeHours: number

  /**
   * 深夜時間。
   * 通常・残業・休日時間と重複し得る。
   */
  nightWorkHours: number

  /**
   * 休日労働時間。
   * workHoursとは分離して保持する。
   */
  holidayWorkHours: number

  allowanceAmount: number
  deductionAmount: number

  allowances: DailyReportAmountItemForm[]
  deductions: DailyReportAmountItemForm[]

  vehicleUsedFlag: boolean
  mileage: number

  paidLeaveDays: number
  paidLeaveRemainingDays: number
  paidLeaveRemainingAfterUsedDays: number

  loanRepaymentAmount: number
  savingAmount: number

  estimatedGrossPayAmount: number
  estimatedNetPayAmount: number

  loanBalance: number
  savingBalance: number

  monthlyLoanRepayment: number
  monthlySavingAmount: number

  approvalStatus: ApprovalStatus
  approvalComment: string
}