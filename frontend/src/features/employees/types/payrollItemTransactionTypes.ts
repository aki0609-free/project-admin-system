export type PayrollItemTransactionStatus = 'DRAFT' | 'CONFIRMED'
export type PayrollItemTransactionPurpose = 'BALANCE_ACCRUAL' | 'PAYROLL_ITEM'

export type EmployeePayrollItemTransaction = {
  id: number
  employeeId: number
  targetType: 'ALLOWANCE' | 'DEDUCTION'
  targetCode: string
  targetName: string
  targetMonth: string
  transactionDate: string
  amount: number
  quantity: number | null
  transactionPurpose: PayrollItemTransactionPurpose
  balanceEffect: 'NONE' | 'CREDIT' | 'DEBIT'
  sourceType: 'MANUAL' | 'CSV' | 'EXTERNAL' | 'MONTHLY_OPERATION'
  sourceReference: string | null
  status: PayrollItemTransactionStatus
  note: string | null
  lockVersion: number
}

export type EmployeePayrollItemTransactionRequest = {
  targetType: 'ALLOWANCE' | 'DEDUCTION'
  targetCode: string
  targetMonth: string
  transactionDate: string
  amount: number
  quantity: number | null
  transactionPurpose: PayrollItemTransactionPurpose
  balanceEffect: 'NONE' | 'CREDIT' | 'DEBIT'
  status: PayrollItemTransactionStatus
  sourceReference: string | null
  note: string | null
}
