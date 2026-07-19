import type {
  DeductionCalculationType,
  DeductionDetailTableRow,
  DeductionDetailViewType,
  DeductionListItem,
  DeductionMaster,
  DeductionType,
  DeductionUnit,
} from '@/features/master/deduction/types/deductionTypes'

import type {
  BaseDeductionDetailResponse,
  DeductionDetailResponse,
  DeductionListItemResponse,
  DeductionSaveRequest,
} from '@/features/master/deduction/types/deductionApiTypes'

import { deductionDetailViewTypeLabelMap } from '@/features/master/deduction/constants/deductionConstants'

function toStringValue(value: string | null | undefined): string {
  return value ?? ''
}

function toNullableString(value: string | null | undefined): string | null {
  if (value == null || value === '') return null
  return value
}

function toDeductionType(value: string | null | undefined): DeductionType {
  if (
    value === 'LEGAL' ||
    value === 'COMPANY' ||
    value === 'EMPLOYEE' ||
    value === 'TEMPORARY' ||
    value === 'ADJUSTMENT' ||
    value === 'OTHER'
  ) {
    return value
  }

  return 'COMPANY'
}

function toCalculationType(value: string | null | undefined): DeductionCalculationType {
  if (value === 'MANUAL' || value === 'FIXED' || value === 'AUTO') {
    return value
  }

  return 'MANUAL'
}

function toDeductionUnit(value: string | null | undefined): DeductionUnit {
  if (
    value === 'DAILY' ||
    value === 'MONTHLY' ||
    value === 'BOTH' ||
    value === 'PAYROLL'
  ) {
    return value
  }

  return 'MONTHLY'
}

function toDetailViewType(value: string | null | undefined): DeductionDetailViewType {
  if (
    value === 'NONE' ||
    value === 'INCOME_TAX' ||
    value === 'RESIDENT_TAX' ||
    value === 'HEALTH_INSURANCE' ||
    value === 'PENSION' ||
    value === 'EMPLOYMENT_INSURANCE'
  ) {
    return value
  }

  return 'NONE'
}

export function toDeductionMaster(
  response: DeductionListItemResponse | DeductionDetailResponse | DeductionListItem,
): DeductionMaster {
  if ('code' in response && 'name' in response) {
    return {
      id: response.id,
      code: response.code,
      name: response.name,
      deductionType: response.deductionType,
      calculationType: response.calculationType,
      deductionUnit: response.deductionUnit,
      detailViewType: response.detailViewType,

      ruleName: response.ruleName,
      defaultAmount: response.defaultAmount,
      allowManualInput: response.allowManualInput,
      minAmount: response.minAmount,
      maxAmount: response.maxAmount,

      showOnDailyStatement: response.showOnDailyStatement,
      showOnMonthlyStatement: response.showOnMonthlyStatement,
      carryToMonthlySettlement: response.carryToMonthlySettlement,
      displayOrder: response.displayOrder,
      enabled: response.enabled,
      note: response.note,
    }
  }

  return {
    id: response.id,
    code: response.deductionCode,
    name: response.deductionName,
    deductionType: toDeductionType(response.deductionType),
    calculationType: toCalculationType(response.calculationType),
    deductionUnit: toDeductionUnit(response.deductionUnit),
    detailViewType: toDetailViewType(response.detailViewType),

    ruleName: toNullableString(response.ruleName),
    defaultAmount: response.defaultAmount,
    allowManualInput: response.allowManualInput ?? true,
    minAmount: response.minAmount,
    maxAmount: response.maxAmount,

    showOnDailyStatement: response.showOnDailyStatement ?? false,
    showOnMonthlyStatement: response.showOnMonthlyStatement ?? true,
    carryToMonthlySettlement: response.carryToMonthlySettlement ?? false,
    displayOrder: response.displayOrder,
    enabled: response.enabled ?? true,
    note: toStringValue(response.note),
  }
}

export function toDeductionListItem(
  response: DeductionListItemResponse,
): DeductionListItem {
  const master = toDeductionMaster(response)

  return {
    ...master,
    detailSummary: deductionDetailViewTypeLabelMap[master.detailViewType] ?? master.detailViewType,
  }
}

export function toDeductionSaveRequest(
  payload: DeductionMaster,
): DeductionSaveRequest {
  return {
    deductionCode: payload.code,
    deductionName: payload.name,
    deductionType: payload.deductionType,
    calculationType: payload.calculationType,
    deductionUnit: payload.deductionUnit,
    detailViewType: payload.detailViewType,

    ruleName: toNullableString(payload.ruleName),
    defaultAmount: payload.defaultAmount,
    allowManualInput: payload.allowManualInput,
    minAmount: payload.minAmount,
    maxAmount: payload.maxAmount,

    showOnDailyStatement: payload.showOnDailyStatement,
    showOnMonthlyStatement: payload.showOnMonthlyStatement,
    carryToMonthlySettlement: payload.carryToMonthlySettlement,
    displayOrder: payload.displayOrder,
    enabled: payload.enabled,
    note: toNullableString(payload.note),
  }
}

export function toDeductionDetailRows(
  details: Record<string, BaseDeductionDetailResponse[]> | null | undefined,
  detailViewType: DeductionDetailViewType,
): DeductionDetailTableRow[] {
  const rows = details?.[detailViewType] ?? []

  return rows.map(row => ({
    id: row.id,
    detailType: row.detailType,
    label: row.label,
    deleteSelected: false,
    ...row.values,
    _isNew: false,
    _isUpdated: false,
    _isDeleted: false,
  }))
}