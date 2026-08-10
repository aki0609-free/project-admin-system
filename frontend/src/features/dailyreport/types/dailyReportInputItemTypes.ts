export type DailyReportInputItemType = 'ALLOWANCE' | 'DEDUCTION'

export type DailyReportInputMode =
  | 'MANUAL'
  | 'FIXED'
  | 'FIXED_WITH_OVERRIDE'
  | 'AUTO_CALCULATED'
  | 'AUTO_WITH_OVERRIDE'

export type DailyReportInputItemResponse = {
  masterId: number
  code: string
  name: string
  itemType: DailyReportInputItemType
  inputMode: DailyReportInputMode
  calculatedAmount: number
  amount: number
  manualOverride: boolean
  overrideReason: string | null
  editable: boolean
  displayOrder: number
  balanceTracked: boolean
  balanceUnit: 'DAYS' | 'HOURS' | 'COUNT' | 'AMOUNT' | null
  openingQuantity: number
  accruedQuantity: number
  consumedQuantity: number
  remainingQuantity: number
  quantity: number
  remainingAfterQuantity: number
}

export type DailyReportInputResponse = {
  allowances: DailyReportInputItemResponse[]
  deductions: DailyReportInputItemResponse[]
}

export type DailyReportAmountItemForm = {
  masterId: number
  code: string
  name: string
  itemType: DailyReportInputItemType
  inputMode: DailyReportInputMode
  calculatedAmount: number
  amount: number
  manualOverride: boolean
  overrideReason: string
  editable: boolean
  displayOrder: number
  balanceTracked: boolean
  balanceUnit: 'DAYS' | 'HOURS' | 'COUNT' | 'AMOUNT' | null
  openingQuantity: number
  accruedQuantity: number
  consumedQuantity: number
  remainingQuantity: number
  quantity: number
  remainingAfterQuantity: number
}
