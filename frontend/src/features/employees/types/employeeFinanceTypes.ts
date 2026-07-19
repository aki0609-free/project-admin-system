export type EmployeeFinanceSummaryResponse = {
  employeeId: number

  savingBalance: number
  loanBalance: number

  monthlySavingAmount: number
  monthlyLoanRepayment: number
}