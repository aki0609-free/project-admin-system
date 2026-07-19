export type DailyReportInputItemType = 'ALLOWANCE' | 'DEDUCTION'

export type DailyReportInputMode = 'MANUAL' | 'FIXED' | 'AUTO_CALCULATED'

export type DailyReportInputItemResponse = {
  masterId: number
  code: string
  name: string
  itemType: DailyReportInputItemType
  inputMode: DailyReportInputMode
  amount: number
  editable: boolean
  displayOrder: number
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
  amount: number
  editable: boolean
  displayOrder: number
}