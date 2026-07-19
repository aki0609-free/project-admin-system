import type { SimpleTableEditableRow } from '@/shared/components/table/simple_table/types/item/types'

export type AllowanceType =
  | 'LEGAL'
  | 'COMPANY'
  | 'EMPLOYEE'
  | 'TEMPORARY'
  | 'ADJUSTMENT'
  | 'OTHER'

export type AllowanceCalculationType =
  | 'MANUAL'
  | 'FIXED'
  | 'AUTO'

export type AllowanceUnit =
  | 'DAILY'
  | 'MONTHLY'
  | 'BOTH'
  | 'PAYROLL'

export type AllowanceDetailViewType = 'NONE'

export type AllowanceMaster = {
  id: number
  code: string
  name: string
  allowanceType: AllowanceType
  calculationType: AllowanceCalculationType
  allowanceUnit: AllowanceUnit
  detailViewType: AllowanceDetailViewType

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
  note: string
}

export type AllowanceListItem = AllowanceMaster & {
  detailLabel: string
}

export type AllowanceDetailTableRow = SimpleTableEditableRow & {
  deleteSelected?: boolean
  detailType?: string
  label?: string
  [key: string]: unknown
}

export type AllowanceSavePayload = AllowanceMaster