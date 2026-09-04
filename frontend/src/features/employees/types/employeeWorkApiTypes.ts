export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED'
export type SalaryType = 'MONTHLY' | 'WEEKLY' | 'DAILY' | 'HOURLY'
export type PaymentCycle = 'DAILY' | 'WEEKLY' | 'MONTHLY'
export type EmployeeFinanceAccountType = 'LOAN' | 'SAVING'
export type EmployeeFinanceTransactionType =
  | 'OPENING_BALANCE'
  | 'LOAN_DISBURSEMENT'
  | 'LOAN_DISBURSEMENT_REVERSAL'
  | 'LOAN_PRINCIPAL_ADJUSTMENT'
  | 'LOAN_REPAYMENT'
  | 'LOAN_REPAYMENT_REVERSAL'
  | 'SAVING_DEPOSIT'
  | 'SAVING_DEPOSIT_REVERSAL'

export type EmployeeContractQueryResponse = {
  id: number | null

  employeeId: number
  employeeCode: string | null
  employeeName: string | null

  contractStartDate: string | null
  contractEndDate: string | null

  salaryType: SalaryType | null
  paymentCycle: PaymentCycle | null

  monthlySalary: number
  weeklyWage: number
  dailyWage: number
  hourlyWage: number

  standardWorkingHours: number

  note: string | null
}

export type EmployeeLoanResponse = {
  id: number
  employeeId: number
  employeeCode: string
  employeeName: string
  principal: number
  currentBalance: number
  monthlyRepayment: number
  loanDate: string | null
  repaymentStartDate: string | null
  activeFlag: boolean
  approvalStatus: ApprovalStatus
  approvalComment: string | null
}

export type EmployeeLoanSaveRequest = {
  employeeId: number
  principal: number
  monthlyRepayment: number
  loanDate: string | null
  repaymentStartDate: string | null
  activeFlag: boolean
}

export type EmployeeSavingResponse = {
  id: number
  employeeId: number
  employeeCode: string
  employeeName: string
  percentage: number
  savingCalculationBaseAmount: number
  currentBalance: number
  activeFlag: boolean
  approvalStatus: ApprovalStatus
  approvalComment: string | null
}

export type EmployeeSavingSaveRequest = {
  employeeId: number
  percentage: number
  savingCalculationBaseAmount: number
  activeFlag: boolean
}

export type EmployeeFinanceTransactionResponse = {
  id: number
  employeeId: number
  employeeCode: string
  employeeName: string
  accountType: EmployeeFinanceAccountType
  transactionType: EmployeeFinanceTransactionType
  accountReferenceId: number
  dailyReportId: number | null
  transactionDate: string
  amount: number
  balanceBefore: number
  balanceAfter: number
  note: string | null
  createdAt: string
}
