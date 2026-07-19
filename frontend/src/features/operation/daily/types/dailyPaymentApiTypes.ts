export type DailyPaymentStatus = 'PENDING' | 'PAID' | 'CANCELLED'

export type DailyPaymentResponse = {
  id: number | null
  paymentDate: string

  employeeId: number
  employeeCode: string
  employeeName: string

  plannedAmount: number
  actualAmount: number

  status: DailyPaymentStatus
  paidAt: string | null
  note: string | null
}

export type DailyPaymentBulkSaveItemRequest = {
  id: number | null
  employeeId: number

  plannedAmount: number
  actualAmount: number

  status: DailyPaymentStatus
  note: string | null

  isNew: boolean
  isUpdated: boolean
  isDeleted: boolean
}

export type DailyPaymentBulkSaveRequest = {
  paymentDate: string
  items: DailyPaymentBulkSaveItemRequest[]
}

export type DailyPaymentDenominationResponse = {
  yen10000: number
  yen5000: number
  yen1000: number
  yen500: number
  yen100: number
  yen50: number
  yen10: number
  yen5: number
  yen1: number
}

export type DailyPaymentPrintDetailResponse = {
  employeeId: number
  employeeCode: string
  employeeName: string
  plannedAmount: number
  actualAmount: number
  note: string | null
  denomination: DailyPaymentDenominationResponse
}

export type DailyPaymentPrintSummaryResponse = {
  paymentDate: string
  employeeCount: number
  totalPlannedAmount: number
  totalActualAmount: number
  totalDenomination: DailyPaymentDenominationResponse
  details: DailyPaymentPrintDetailResponse[]
}