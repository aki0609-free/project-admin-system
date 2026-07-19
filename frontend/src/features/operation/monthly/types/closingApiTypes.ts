export type MonthlyClosingResponse = {
  id: number

  targetMonth: string

  status: 'OPEN' | 'CLOSED'

  closingVersion: number

  closingStartDate: string | null
  closingEndDate: string | null

  closingRuleType: string | null
  closingRuleValue: number | null

  closedAt: string | null
  closedBy: string | null

  note: string | null
}

export type MonthlyClosingSummaryResponse = {
  targetMonth: string
  employeeCount: number
  workReportCount: number
  totalGrossAmount: number
  totalDeductionAmount: number
  totalDailyPaymentAmount: number
  totalNetPaymentAmount: number
  closing: MonthlyClosingResponse | null
}

export type MonthlyPayrollSummaryResponse = {
  employeeId: number
  employeeCode: string
  employeeName: string
  workDayCount: number
  totalWorkHours: number
  totalOvertimeHours: number
  totalNightWorkHours: number
  allowanceAmount: number
  deductionAmount: number
  savingAmount: number
  loanRepaymentAmount: number
  grossAmount: number
  dailyPaymentAmount: number
  netPaymentAmount: number
}

export type MonthlyBillingSummaryResponse = {
  customerId: number
  customerName: string
  customerSiteId: number
  siteName: string

  workReportCount: number
  workerCount: number

  totalWorkHours: number
  totalOvertimeHours: number
  totalNightWorkHours: number

  unitPrice: number
  billingAmount: number
}