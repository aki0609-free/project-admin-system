export type EmployeeLoanForm = {
  id: number
  employeeId: number | null
  principal: number
  currentBalance: number
  monthlyRepayment: number
  loanDate: string
  repaymentStartDate: string
  activeFlag: boolean
}

export type EmployeeSavingForm = {
  id: number
  employeeId: number | null
  percentage: number
  minSalaryThreshold: number
  currentBalance: number
  activeFlag: boolean
}
