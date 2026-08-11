export type PayrollItemTransactionStatus = 'DRAFT' | 'CONFIRMED'

export type EmployeePayrollItemTransaction = {
  id: number
  employeeId: number
  targetCode: string
  targetName: string
  targetMonth: string
  transactionDate: string
  amount: number
  quantity: number | null
  sourceType: 'MANUAL' | 'CSV' | 'EXTERNAL' | 'MONTHLY_OPERATION'
  sourceReference: string | null
  status: PayrollItemTransactionStatus
  note: string | null
  lockVersion: number
}

export type EmployeePayrollItemTransactionRequest = {
  targetCode: string
  targetMonth: string
  transactionDate: string
  amount: number
  quantity: number | null
  status: PayrollItemTransactionStatus
  sourceReference: string | null
  note: string | null
}
