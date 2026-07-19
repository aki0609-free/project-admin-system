export type Gender = 'MALE' | 'FEMALE' | 'OTHER'

export type EmploymentType =
  | 'FULL_TIME'
  | 'CONTRACT'
  | 'PART_TIME'
  | 'TEMPORARY'
  | 'DAILY_WORKER'

export type EmploymentStatus =
  | 'ACTIVE'
  | 'LEAVE'
  | 'RESIGNED'

export type TaxCategory =
  | 'KOU'
  | 'OTSU'
  | 'HEI'

export type SalaryType =
  | 'MONTHLY'
  | 'WEEKLY'
  | 'DAILY'
  | 'HOURLY'

  export type PaymentCycle =
  | 'DAILY'
  | 'WEEKLY'
  | 'MONTHLY'

export type EmployeeListItemResponse = {
  id: number
  employeeCode: string
  employeeName: string
  employeeNameKana: string | null
  gender: Gender | null
  birthDate: string | null
  hireDate: string | null
  resignDate: string | null
  employmentType: EmploymentType
  employmentStatus: EmploymentStatus
  phone: string | null
  email: string | null
  activeFlag: boolean
}

export type EmployeePayrollProfileResponse = {
  id: number | null
  taxCategory: TaxCategory
  taxDependentCount: number

  dependentFlag: boolean
  dependentOfOtherFlag: boolean
  paidLeaveRemainingDays: number

  incomeTaxCalcFlag: boolean
  residentTaxCalcFlag: boolean
  residentTaxMonthly: number
  employmentInsuranceFlag: boolean
  socialInsuranceFlag: boolean
  healthInsuranceFlag: boolean
  pensionInsuranceFlag: boolean
  careInsuranceFlag: boolean
  dailyPayFlag: boolean
  commuteAllowanceMonthly: number
}

export type EmployeeContractResponse = {
  id: number | null
  contractStartDate: string | null
  contractEndDate: string | null
  renewalFlag: boolean
  salaryType: SalaryType
  paymentCycle: PaymentCycle
  monthlySalary: number
  weeklyWage: number
  dailyWage: number
  hourlyWage: number
  standardWorkingHours: number
  note: string | null
}

export type EmployeeDetailResponse = EmployeeListItemResponse & {
  postalCode: string | null
  address: string | null
  payrollProfile: EmployeePayrollProfileResponse
  contract: EmployeeContractResponse
}

export type EmployeePayrollProfileSaveRequest = {
  taxCategory: TaxCategory
  taxDependentCount: number

  dependentFlag: boolean
  dependentOfOtherFlag: boolean
  paidLeaveRemainingDays: number

  incomeTaxCalcFlag: boolean
  residentTaxCalcFlag: boolean
  residentTaxMonthly: number
  employmentInsuranceFlag: boolean
  socialInsuranceFlag: boolean
  healthInsuranceFlag: boolean
  pensionInsuranceFlag: boolean
  careInsuranceFlag: boolean
  dailyPayFlag: boolean
  commuteAllowanceMonthly: number
}

export type EmployeeContractSaveRequest = {
  contractStartDate: string | null
  contractEndDate: string | null
  renewalFlag: boolean
  salaryType: SalaryType
  paymentCycle: PaymentCycle
  monthlySalary: number
  weeklyWage: number
  dailyWage: number
  hourlyWage: number
  standardWorkingHours: number
  note: string | null
}

export type EmployeeSaveRequest = {
  employeeCode: string
  employeeName: string
  employeeNameKana: string | null
  gender: Gender | null
  birthDate: string | null
  hireDate: string | null
  resignDate: string | null
  employmentType: EmploymentType
  employmentStatus: EmploymentStatus
  phone: string | null
  email: string | null
  postalCode: string | null
  address: string | null
  activeFlag: boolean
  payrollProfile: EmployeePayrollProfileSaveRequest
  contract: EmployeeContractSaveRequest
}

export type EmployeeResignationChecklistResponse = {
  id: number
  code: string
  name: string
  description: string | null
  requiredFlag: boolean
  displayOrder: number
}

export type EmployeeResignRequest = {
  resignDate: string
  checkedChecklistIds: number[]
  note: string | null
}