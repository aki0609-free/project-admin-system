import type {
  EmployeeLoanForm,
  EmployeeSavingForm,
} from '../types/employeeLoanSavingFormTypes'
import type {
  EmployeeLoanResponse,
  EmployeeSavingResponse,
} from '../types/employeeWorkApiTypes'

export const createEmptyEmployeeLoanForm = (): EmployeeLoanForm => ({
  id: 0,
  employeeId: null,
  principal: 0,
  currentBalance: 0,
  monthlyRepayment: 0,
  loanDate: '',
  repaymentStartDate: '',
  activeFlag: true,
  approvalStatus: 'PENDING',
  approvalComment: '',
})

export const toEmployeeLoanForm = (
  item: EmployeeLoanResponse,
): EmployeeLoanForm => ({
  id: item.id,
  employeeId: item.employeeId,
  principal: item.principal ?? 0,
  currentBalance: item.currentBalance ?? 0,
  monthlyRepayment: item.monthlyRepayment ?? 0,
  loanDate: item.loanDate ?? '',
  repaymentStartDate: item.repaymentStartDate ?? '',
  activeFlag: item.activeFlag,
  approvalStatus: item.approvalStatus,
  approvalComment: item.approvalComment ?? '',
})

export const createEmptyEmployeeSavingForm = (): EmployeeSavingForm => ({
  id: 0,
  employeeId: null,
  percentage: 0,
  minSalaryThreshold: 0,
  currentBalance: 0,
  activeFlag: true,
  approvalStatus: 'APPROVED',
  approvalComment: '',
})

export const toEmployeeSavingForm = (
  item: EmployeeSavingResponse,
): EmployeeSavingForm => ({
  id: item.id,
  employeeId: item.employeeId,
  percentage: item.percentage ?? 0,
  minSalaryThreshold: item.minSalaryThreshold ?? 0,
  currentBalance: item.currentBalance ?? 0,
  activeFlag: item.activeFlag,
  approvalStatus: item.approvalStatus,
  approvalComment: item.approvalComment ?? '',
})