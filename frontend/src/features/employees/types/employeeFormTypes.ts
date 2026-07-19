import type {
  EmploymentStatus,
  EmploymentType,
  Gender,
  PaymentCycle,
  SalaryType,
  TaxCategory,
} from './employeeApiTypes'

export type EmployeePayrollProfileForm = {
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

export type EmployeeContractForm = {
  contractStartDate: string
  contractEndDate: string
  renewalFlag: boolean
  salaryType: SalaryType
  paymentCycle: PaymentCycle
  monthlySalary: number
  weeklyWage: number
  dailyWage: number
  hourlyWage: number
  standardWorkingHours: number
  note: string
}

export type EmployeeForm = {
  id: number
  employeeCode: string
  employeeName: string
  employeeNameKana: string
  gender: Gender | null
  birthDate: string
  hireDate: string
  resignDate: string
  employmentType: EmploymentType
  employmentStatus: EmploymentStatus
  phone: string
  email: string
  postalCode: string
  address: string
  activeFlag: boolean
  payrollProfile: EmployeePayrollProfileForm
  contract: EmployeeContractForm
}