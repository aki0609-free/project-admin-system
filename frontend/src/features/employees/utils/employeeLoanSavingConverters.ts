import type {
  EmployeeLoanForm,
  EmployeeSavingForm,
} from '../types/employeeLoanSavingFormTypes'
import type {
  EmployeeLoanSaveRequest,
  EmployeeSavingSaveRequest,
} from '../types/employeeWorkApiTypes'

const blankToNull = (value: string): string | null =>
  value.trim() ? value : null

export const toEmployeeLoanSaveRequest = (
  form: EmployeeLoanForm,
): EmployeeLoanSaveRequest => {
  if (form.employeeId == null) {
    throw new Error('employeeId is required.')
  }

  return {
    employeeId: form.employeeId,
    principal: form.principal,
    currentBalance: form.currentBalance,
    monthlyRepayment: form.monthlyRepayment,
    loanDate: blankToNull(form.loanDate),
    repaymentStartDate: blankToNull(form.repaymentStartDate),
    activeFlag: form.activeFlag,
    approvalStatus: form.approvalStatus,
    approvalComment: blankToNull(form.approvalComment),
  }
}

export const toEmployeeSavingSaveRequest = (
  form: EmployeeSavingForm,
): EmployeeSavingSaveRequest => {
  if (form.employeeId == null) {
    throw new Error('employeeId is required.')
  }

  return {
    employeeId: form.employeeId,
    percentage: form.percentage,
    minSalaryThreshold: form.minSalaryThreshold,
    currentBalance: form.currentBalance,
    activeFlag: form.activeFlag,
    approvalStatus: form.approvalStatus,
    approvalComment: blankToNull(form.approvalComment),
  }
}