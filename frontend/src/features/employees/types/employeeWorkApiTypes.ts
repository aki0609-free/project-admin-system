export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED'
export type SalaryType = 'MONTHLY' | 'WEEKLY' | 'DAILY' | 'HOURLY'
export type PaymentCycle = 'DAILY' | 'WEEKLY' | 'MONTHLY'

export type EmployeeContractQueryResponse = {
  id: number | null

  employeeId: number
  employeeCode: string | null
  employeeName: string | null

  contractStartDate: string | null
  contractEndDate: string | null
  renewalFlag: boolean

  salaryType: SalaryType | null
  paymentCycle: PaymentCycle | null

  monthlySalary: number
  weeklyWage: number
  dailyWage: number
  hourlyWage: number

  standardWorkingHours: number

  note: string | null
}

export type EmployeeTimesheetResponse = {
  id: number
  employeeId: number
  employeeCode: string
  employeeName: string
  workDate: string
  clockIn: string | null
  clockOut: string | null
  workHours: number
  overtimeHours: number
  nightShiftHours: number
  weekendFlag: boolean
  approvalStatus: ApprovalStatus
  approvalComment: string | null
}

export type EmployeeTimesheetSaveRequest = {
  employeeId: number
  workDate: string
  clockIn: string | null
  clockOut: string | null
  workHours: number
  overtimeHours: number
  nightShiftHours: number
  weekendFlag: boolean
  approvalStatus: ApprovalStatus
  approvalComment: string | null
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
  currentBalance: number
  monthlyRepayment: number
  loanDate: string | null
  repaymentStartDate: string | null
  activeFlag: boolean
  approvalStatus: ApprovalStatus
  approvalComment: string | null
}

export type EmployeeSavingResponse = {
  id: number
  employeeId: number
  employeeCode: string
  employeeName: string
  percentage: number
  minSalaryThreshold: number
  currentBalance: number
  activeFlag: boolean
  approvalStatus: ApprovalStatus
  approvalComment: string | null
}

export type EmployeeSavingSaveRequest = {
  employeeId: number
  percentage: number
  minSalaryThreshold: number
  currentBalance: number
  activeFlag: boolean
  approvalStatus: ApprovalStatus
  approvalComment: string | null
}
