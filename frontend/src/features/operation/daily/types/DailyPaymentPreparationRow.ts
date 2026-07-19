export type DailyPaymentType =
  | 'DAILY'
  | 'WEEKLY'
  | 'MONTHLY'

export type DailyPaymentPreparationRow = {
  id: number

  employeeCode: string
  employeeName: string

  paymentType: DailyPaymentType

  grossAmount: number
  taxableAmount: number
  deductionAmount: number
  expenseAmount: number
  actualPaymentAmount: number

  workDayCount: number
  note: string | null
}