export type ApprovalStatus =
  | 'PENDING'
  | 'APPROVED'
  | 'REJECTED'

export type SalaryType =
  | 'MONTHLY'
  | 'WEEKLY'
  | 'DAILY'
  | 'HOURLY'

export type CustomerBillingUnit =
  | 'HOURLY'
  | 'DAILY'
  | 'MONTHLY'
  | 'FIXED'

export type DailyReportAllowanceResponse = {
  id: number
  allowanceMasterId: number
  allowanceCode: string
  allowanceName: string
  amount: number
}

export type DailyReportDeductionResponse = {
  id: number
  deductionMasterId: number
  deductionCode: string
  deductionName: string
  amount: number
}

export type DailyReportResponse = {
  id: number

  employeeId: number
  employeeCode: string
  employeeName: string

  workDate: string
  paymentDate: string | null

  customerId: number | null
  customerSiteId: number | null

  customerName: string | null
  siteName: string | null

  /**
   * 日報保存時に適用された請求単価マスタID。
   */
  billingRateId: number | null

  /**
   * 日報保存時点の職種スナップショット。
   */
  jobCode: string | null
  jobName: string | null

  /**
   * 日報保存時点の現場役職スナップショット。
   */
  siteRoleCode: string | null
  siteRoleName: string | null

  billingUnit: CustomerBillingUnit | null

  /**
   * 日報保存時点の請求単価スナップショット。
   */
  billingBaseUnitPrice: number
  billingOvertimeUnitPrice: number
  billingNightUnitPrice: number
  billingHolidayUnitPrice: number
  billingCommuteUnitPrice: number

  workDescription: string | null

  startTime: string | null
  endTime: string | null
  breakMinutes: number

  /**
   * 通常時間と休日時間は分離して保持する。
   * 深夜時間は時間帯区分なので、他の時間と重複し得る。
   */
  workHours: number
  overtimeHours: number
  nightWorkHours: number
  holidayWorkHours: number

  allowanceAmount: number
  deductionAmount: number

  vehicleUsedFlag: boolean
  mileage: number

  paidLeaveDays: number
  paidLeaveRemainingDays: number
  paidLeaveRemainingAfterUsedDays: number

  loanRepaymentAmount: number
  savingAmount: number

  estimatedGrossPayAmount: number
  estimatedNetPayAmount: number

  /**
   * 従業員財務情報から画面表示用に取得する値。
   */
  loanBalance: number
  savingBalance: number
  monthlyLoanRepayment: number
  monthlySavingAmount: number

  approvalStatus: ApprovalStatus
  approvalComment: string | null
}

export type DailyReportDetailResponse =
  DailyReportResponse & {
    allowances: DailyReportAllowanceResponse[]
    deductions: DailyReportDeductionResponse[]
  }

export type DailyReportAllowanceSaveRequest = {
  allowanceMasterId: number
  allowanceCode: string
  allowanceName: string
  amount: number
}

export type DailyReportDeductionSaveRequest = {
  deductionMasterId: number
  deductionCode: string
  deductionName: string
  amount: number
}

export type DailyReportSaveRequest = {
  employeeId: number

  workDate: string
  paymentDate: string | null

  customerId: number | null
  customerSiteId: number | null

  customerName: string | null
  siteName: string | null

  /**
   * billingRateIdと各請求単価は送信しない。
   * サーバー側で現場・職種・役職・勤務日から再解決する。
   */
  jobCode: string | null
  jobName: string | null

  siteRoleCode: string | null
  siteRoleName: string | null

  workDescription: string | null

  startTime: string | null
  endTime: string | null
  breakMinutes: number

  workHours: number
  overtimeHours: number
  nightWorkHours: number
  holidayWorkHours: number

  allowanceAmount: number
  deductionAmount: number

  vehicleUsedFlag: boolean
  mileage: number

  paidLeaveDays: number

  approvalStatus: ApprovalStatus
  approvalComment: string | null

  loanRepaymentAmount: number
  savingAmount: number

  allowances: DailyReportAllowanceSaveRequest[]
  deductions: DailyReportDeductionSaveRequest[]
}

export type DailyReportMissingEmployeeResponse = {
  employeeId: number
  employeeCode: string
  employeeName: string
}

export type DailyReportMonthlyAttendanceResponse = {
  employeeId: number
  employeeCode: string
  employeeName: string

  targetMonth: string
  periodStart: string
  periodEnd: string

  salaryType: SalaryType | null
  baseSalaryAmount: number
  grossSalaryAmount: number

  reportCount: number

  paidLeaveUsedDays: number
  paidLeaveRemainingDays: number
  paidLeaveRemainingAfterUsedDays: number

  totalWorkHours: number
  totalOvertimeHours: number
  totalNightWorkHours: number
  totalHolidayWorkHours: number

  totalAllowanceAmount: number
  totalDeductionAmount: number

  totalSavingAmount: number
  totalLoanRepaymentAmount: number

  estimatedPaymentAmount: number
}

export type DailyReportEstimatedPayPreviewResponse = {
  estimatedBasePayAmount: number
  estimatedGrossPayAmount: number
  estimatedNetPayAmount: number
}