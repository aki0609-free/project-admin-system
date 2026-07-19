export type BaseDeductionDetailResponse = {
  id: number
  detailType: string
  label: string
  values: Record<string, unknown>
}

export type DeductionListItemResponse = {
  id: number
  deductionCode: string
  deductionName: string
  deductionType: string | null
  calculationType: string | null
  deductionUnit: string | null
  detailViewType: string | null

  ruleName: string | null
  defaultAmount: number | null
  allowManualInput: boolean | null
  minAmount: number | null
  maxAmount: number | null

  showOnDailyStatement: boolean | null
  showOnMonthlyStatement: boolean | null
  carryToMonthlySettlement: boolean | null
  displayOrder: number | null
  enabled: boolean | null
  note: string | null
}

export type DeductionDetailResponse = DeductionListItemResponse & {
  details: Record<string, BaseDeductionDetailResponse[]>
}

export type DeductionSaveRequest = {
  deductionCode: string
  deductionName: string
  deductionType: string
  calculationType: string
  deductionUnit: string
  detailViewType: string

  ruleName: string | null
  defaultAmount: number | null
  allowManualInput: boolean
  minAmount: number | null
  maxAmount: number | null

  showOnDailyStatement: boolean
  showOnMonthlyStatement: boolean
  carryToMonthlySettlement: boolean
  displayOrder: number | null
  enabled: boolean
  note: string | null
}