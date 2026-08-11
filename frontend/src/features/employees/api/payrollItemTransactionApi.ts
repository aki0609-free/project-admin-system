import { del, get, post, put } from '@/shared/api/http'
import type {
  EmployeePayrollItemTransaction,
  EmployeePayrollItemTransactionRequest,
} from '../types/payrollItemTransactionTypes'

const basePath = '/api/employees/{employeeId}/payroll-item-transactions'

export const payrollItemTransactionApi = {
  findAll: (
    employeeId: number,
    targetCode: string,
    targetMonth: string,
  ) => get<EmployeePayrollItemTransaction[]>(basePath, {
    params: {
      path: { employeeId },
      query: { targetCode, targetMonth },
    },
  }),

  create: (
    employeeId: number,
    request: EmployeePayrollItemTransactionRequest,
  ) => post<EmployeePayrollItemTransaction, EmployeePayrollItemTransactionRequest>(
    basePath,
    request,
    { params: { path: { employeeId } } },
  ),

  update: (
    employeeId: number,
    transactionId: number,
    request: EmployeePayrollItemTransactionRequest,
  ) => put<EmployeePayrollItemTransaction, EmployeePayrollItemTransactionRequest>(
    '/api/employees/{employeeId}/payroll-item-transactions/{transactionId}',
    request,
    { params: { path: { employeeId, transactionId } } },
  ),

  remove: (employeeId: number, transactionId: number) => del(
    '/api/employees/{employeeId}/payroll-item-transactions/{transactionId}',
    { params: { path: { employeeId, transactionId } } },
  ),
}
