import type { EmployeeDetailResponse, EmployeeSaveRequest } from '../types/employeeApiTypes'
import type { EmployeeForm } from '../types/employeeFormTypes'

const blankToNull = (value: string): string | null => (value.trim() ? value : null)

export const createEmptyEmployeeForm = (): EmployeeForm => ({
  id: 0,

  employeeCode: '',
  employeeName: '',
  employeeNameKana: '',
  gender: null,
  birthDate: '',
  hireDate: '',
  resignDate: '',
  employmentType: 'FULL_TIME',
  employmentStatus: 'ACTIVE',
  phone: '',
  email: '',
  postalCode: '',
  address: '',
  activeFlag: true,

  payrollProfile: {
    taxCategory: 'KOU',
    taxDependentCount: 0,

    dependentFlag: false,
    dependentOfOtherFlag: false,
    paidLeaveRemainingDays: 0,

    incomeTaxCalcFlag: true,
    residentTaxCalcFlag: true,
    residentTaxMonthly: 0,
    employmentInsuranceFlag: true,
    socialInsuranceFlag: true,
    healthInsuranceFlag: true,
    pensionInsuranceFlag: true,
    careInsuranceFlag: false,
    dailyPayFlag: false,
    commuteAllowanceMonthly: 0,
  },

  contract: {
    contractStartDate: '',
    contractEndDate: '',
    renewalFlag: false,
    salaryType: 'MONTHLY',
    paymentCycle: 'MONTHLY',
    monthlySalary: 0,
    weeklyWage: 0,
    dailyWage: 0,
    hourlyWage: 0,
    standardWorkingHours: 0,
    note: '',
  },
})

export const toEmployeeForm = (detail: EmployeeDetailResponse): EmployeeForm => ({
  id: detail.id,

  employeeCode: detail.employeeCode,
  employeeName: detail.employeeName,
  employeeNameKana: detail.employeeNameKana ?? '',
  gender: detail.gender,
  birthDate: detail.birthDate ?? '',
  hireDate: detail.hireDate ?? '',
  resignDate: detail.resignDate ?? '',
  employmentType: detail.employmentType,
  employmentStatus: detail.employmentStatus,
  phone: detail.phone ?? '',
  email: detail.email ?? '',
  postalCode: detail.postalCode ?? '',
  address: detail.address ?? '',
  activeFlag: detail.activeFlag,

  payrollProfile: {
    taxCategory: detail.payrollProfile.taxCategory,
    taxDependentCount: detail.payrollProfile.taxDependentCount,

    dependentFlag: detail.payrollProfile.dependentFlag,
    dependentOfOtherFlag: detail.payrollProfile.dependentOfOtherFlag,
    paidLeaveRemainingDays: detail.payrollProfile.paidLeaveRemainingDays ?? 0,

    incomeTaxCalcFlag: detail.payrollProfile.incomeTaxCalcFlag,
    residentTaxCalcFlag: detail.payrollProfile.residentTaxCalcFlag,
    residentTaxMonthly: detail.payrollProfile.residentTaxMonthly ?? 0,
    employmentInsuranceFlag: detail.payrollProfile.employmentInsuranceFlag,
    socialInsuranceFlag: detail.payrollProfile.socialInsuranceFlag,
    healthInsuranceFlag: detail.payrollProfile.healthInsuranceFlag,
    pensionInsuranceFlag: detail.payrollProfile.pensionInsuranceFlag,
    careInsuranceFlag: detail.payrollProfile.careInsuranceFlag,
    dailyPayFlag: detail.payrollProfile.dailyPayFlag,
    commuteAllowanceMonthly: detail.payrollProfile.commuteAllowanceMonthly ?? 0,
  },

  contract: {
    contractStartDate: detail.contract.contractStartDate ?? '',
    contractEndDate: detail.contract.contractEndDate ?? '',
    renewalFlag: detail.contract.renewalFlag,
    salaryType: detail.contract.salaryType,
    paymentCycle: detail.contract.paymentCycle ?? 'MONTHLY',
    monthlySalary: detail.contract.monthlySalary ?? 0,
    weeklyWage: detail.contract.weeklyWage ?? 0,
    dailyWage: detail.contract.dailyWage ?? 0,
    hourlyWage: detail.contract.hourlyWage ?? 0,
    standardWorkingHours: detail.contract.standardWorkingHours ?? 0,
    note: detail.contract.note ?? '',
  },
})

export const toEmployeeSaveRequest = (form: EmployeeForm): EmployeeSaveRequest => ({
  employeeCode: form.employeeCode,
  employeeName: form.employeeName,
  employeeNameKana: blankToNull(form.employeeNameKana),
  gender: form.gender,
  birthDate: blankToNull(form.birthDate),
  hireDate: blankToNull(form.hireDate),
  resignDate: blankToNull(form.resignDate),
  employmentType: form.employmentType,
  employmentStatus: form.employmentStatus,
  phone: blankToNull(form.phone),
  email: blankToNull(form.email),
  postalCode: blankToNull(form.postalCode),
  address: blankToNull(form.address),
  activeFlag: form.activeFlag,

  payrollProfile: {
    taxCategory: form.payrollProfile.taxCategory,
    taxDependentCount: form.payrollProfile.taxDependentCount,

    dependentFlag: form.payrollProfile.dependentFlag,
    dependentOfOtherFlag: form.payrollProfile.dependentOfOtherFlag,
    paidLeaveRemainingDays: form.payrollProfile.paidLeaveRemainingDays,

    incomeTaxCalcFlag: form.payrollProfile.incomeTaxCalcFlag,
    residentTaxCalcFlag: form.payrollProfile.residentTaxCalcFlag,
    residentTaxMonthly: form.payrollProfile.residentTaxMonthly,
    employmentInsuranceFlag: form.payrollProfile.employmentInsuranceFlag,
    socialInsuranceFlag: form.payrollProfile.socialInsuranceFlag,
    healthInsuranceFlag: form.payrollProfile.healthInsuranceFlag,
    pensionInsuranceFlag: form.payrollProfile.pensionInsuranceFlag,
    careInsuranceFlag: form.payrollProfile.careInsuranceFlag,
    dailyPayFlag: form.payrollProfile.dailyPayFlag,
    commuteAllowanceMonthly: form.payrollProfile.commuteAllowanceMonthly,
  },

  contract: {
    contractStartDate: blankToNull(form.contract.contractStartDate),
    contractEndDate: blankToNull(form.contract.contractEndDate),
    renewalFlag: form.contract.renewalFlag,
    salaryType: form.contract.salaryType,
    paymentCycle: form.contract.paymentCycle,
    monthlySalary: form.contract.monthlySalary,
    weeklyWage: form.contract.weeklyWage,
    dailyWage: form.contract.dailyWage,
    hourlyWage: form.contract.hourlyWage,
    standardWorkingHours: form.contract.standardWorkingHours,
    note: blankToNull(form.contract.note),
  },
})
