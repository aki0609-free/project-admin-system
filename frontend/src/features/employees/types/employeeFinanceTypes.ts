export type EmployeeFinanceSummaryResponse = {
  employeeId: number

  hasActiveLoan: boolean
  hasActiveSaving: boolean

  savingBalance: number
  loanBalance: number

  monthlySavingAmount: number
  monthlyLoanRepayment: number
}
