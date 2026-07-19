export type BaseAllowanceDetailResponse = {
  id: number
  detailType: string
  label: string
  values: Record<string, unknown>
}

export type AllowanceListItemResponse = {
  id: number
  allowanceCode: string
  allowanceName: string
  allowanceType: string | null
  calculationType: string | null
  allowanceUnit: string | null
  detailViewType: string | null

  ruleName: string | null
  defaultAmount: number | null
  allowManualInput: boolean | null
  minAmount: number | null
  maxAmount: number | null

  taxable: boolean | null
  showOnDailyStatement: boolean | null
  showOnMonthlyStatement: boolean | null
  displayOrder: number | null
  enabled: boolean | null
  note: string | null
}

export type AllowanceDetailResponse = AllowanceListItemResponse & {
  details: Record<string, BaseAllowanceDetailResponse[]>
}

export type AllowanceSaveRequest = {
  allowanceCode: string
  allowanceName: string
  allowanceType: string
  calculationType: string
  allowanceUnit: string
  detailViewType: string

  ruleName: string | null
  defaultAmount: number | null
  allowManualInput: boolean
  minAmount: number | null
  maxAmount: number | null

  taxable: boolean
  showOnDailyStatement: boolean
  showOnMonthlyStatement: boolean
  displayOrder: number | null
  enabled: boolean
  note: string | null
}