import type {
  AllowanceCalculationType,
  AllowanceDetailTableRow,
  AllowanceDetailViewType,
  AllowanceListItem,
  AllowanceMaster,
  AllowanceType,
  AllowanceUnit,
} from '@/features/master/allowance/types/allowanceTypes'

import type {
  AllowanceDetailResponse,
  AllowanceListItemResponse,
  AllowanceSaveRequest,
  BaseAllowanceDetailResponse,
} from '@/features/master/allowance/types/allowanceApiTypes'

import { allowanceDetailViewTypeLabelMap } from '@/features/master/allowance/constants/allowanceConstants'

function toStringValue(value: string | null | undefined): string {
  return value ?? ''
}

function toNullableString(value: string | null | undefined): string | null {
  if (value == null || value === '') return null
  return value
}

function toAllowanceType(value: string | null | undefined): AllowanceType {
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

function toCalculationType(value: string | null | undefined): AllowanceCalculationType {
  if (value === 'MANUAL' || value === 'FIXED' || value === 'AUTO') {
    return value
  }

  return 'MANUAL'
}

function toAllowanceUnit(value: string | null | undefined): AllowanceUnit {
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

function toDetailViewType(value: string | null | undefined): AllowanceDetailViewType {
  if (value === 'NONE') return value
  return 'NONE'
}

export function toAllowanceMaster(
  response: AllowanceListItemResponse | AllowanceDetailResponse | AllowanceListItem,
): AllowanceMaster {
  if ('code' in response && 'name' in response) {
    return {
      id: response.id,
      code: response.code,
      name: response.name,
      allowanceType: response.allowanceType,
      calculationType: response.calculationType,
      allowanceUnit: response.allowanceUnit,
      detailViewType: response.detailViewType,

      ruleName: response.ruleName,
      defaultAmount: response.defaultAmount,
      allowManualInput: response.allowManualInput,
      minAmount: response.minAmount,
      maxAmount: response.maxAmount,

      taxable: response.taxable,
      showOnDailyStatement: response.showOnDailyStatement,
      showOnMonthlyStatement: response.showOnMonthlyStatement,
      displayOrder: response.displayOrder,
      enabled: response.enabled,
      note: response.note,
    }
  }

  return {
    id: response.id,
    code: response.allowanceCode,
    name: response.allowanceName,
    allowanceType: toAllowanceType(response.allowanceType),
    calculationType: toCalculationType(response.calculationType),
    allowanceUnit: toAllowanceUnit(response.allowanceUnit),
    detailViewType: toDetailViewType(response.detailViewType),

    ruleName: toNullableString(response.ruleName),
    defaultAmount: response.defaultAmount,
    allowManualInput: response.allowManualInput ?? true,
    minAmount: response.minAmount,
    maxAmount: response.maxAmount,

    taxable: response.taxable ?? true,
    showOnDailyStatement: response.showOnDailyStatement ?? false,
    showOnMonthlyStatement: response.showOnMonthlyStatement ?? true,
    displayOrder: response.displayOrder,
    enabled: response.enabled ?? true,
    note: toStringValue(response.note),
  }
}

export function toAllowanceListItem(
  response: AllowanceListItemResponse,
): AllowanceListItem {
  const master = toAllowanceMaster(response)

  return {
    ...master,
    detailLabel: allowanceDetailViewTypeLabelMap[master.detailViewType] ?? master.detailViewType,
  }
}

export function toAllowanceSaveRequest(
  payload: AllowanceMaster,
): AllowanceSaveRequest {
  return {
    allowanceCode: payload.code,
    allowanceName: payload.name,
    allowanceType: payload.allowanceType,
    calculationType: payload.calculationType,
    allowanceUnit: payload.allowanceUnit,
    detailViewType: payload.detailViewType,

    ruleName: toNullableString(payload.ruleName),
    defaultAmount: payload.defaultAmount,
    allowManualInput: payload.allowManualInput,
    minAmount: payload.minAmount,
    maxAmount: payload.maxAmount,

    taxable: payload.taxable,
    showOnDailyStatement: payload.showOnDailyStatement,
    showOnMonthlyStatement: payload.showOnMonthlyStatement,
    displayOrder: payload.displayOrder,
    enabled: payload.enabled,
    note: toNullableString(payload.note),
  }
}

export function toAllowanceDetailRows(
  details: Record<string, BaseAllowanceDetailResponse[]> | null | undefined,
  detailViewType: AllowanceDetailViewType,
): AllowanceDetailTableRow[] {
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