import type { DailyPaymentType } from './DailyPaymentPreparationRow'

export type DailyLaborCostRow = {
  id: number

  employeeCode: string
  employeeName: string

  paymentType: DailyPaymentType

  grossAmount: number
  paymentAmount: number

  note: string | null
}