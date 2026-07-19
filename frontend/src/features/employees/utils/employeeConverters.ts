import { EmployeeSaveRequest } from "../types/employeeApiTypes"
import { EmployeeForm } from "../types/employeeFormTypes"

const blankToNull = (value: string): string | null =>
  value.trim() ? value : null

export const toEmployeeSaveRequest = (
  form: EmployeeForm,
): EmployeeSaveRequest => ({
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
    ...form.payrollProfile,
  },
  contract: {
    ...form.contract,
    contractStartDate: blankToNull(form.contract.contractStartDate),
    contractEndDate: blankToNull(form.contract.contractEndDate),
    note: blankToNull(form.contract.note),
  },
})