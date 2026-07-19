import type { SimpleTableEditableRow } from '@/shared/components/table/simple_table/types/item/types'

export type DeductionType =
  | 'LEGAL'
  | 'COMPANY'
  | 'EMPLOYEE'
  | 'TEMPORARY'
  | 'ADJUSTMENT'
  | 'OTHER'

export type DeductionCalculationType =
  | 'MANUAL'
  | 'FIXED'
  | 'AUTO'

export type DeductionUnit =
  | 'DAILY'
  | 'MONTHLY'
  | 'BOTH'
  | 'PAYROLL'

export type DeductionDetailViewType =
  | 'NONE'
  | 'INCOME_TAX'
  | 'RESIDENT_TAX'
  | 'HEALTH_INSURANCE'
  | 'PENSION'
  | 'EMPLOYMENT_INSURANCE'

export type DeductionMaster = {
  id: number
  code: string
  name: string
  deductionType: DeductionType
  calculationType: DeductionCalculationType
  deductionUnit: DeductionUnit
  detailViewType: DeductionDetailViewType

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
  note: string
}

export type DeductionListItem = DeductionMaster & {
  detailSummary: string
}

export type DeductionDetailTableRow = SimpleTableEditableRow & {
  deleteSelected?: boolean
  detailType?: string
  label?: string
  [key: string]: unknown
}

export type DeductionSavePayload = DeductionMaster