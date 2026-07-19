import type {
  CustomerSiteBillingRateRequest,
  CustomerSiteBillingRateResponse,
} from '../types/customerApiTypes'

import type {
  CustomerSiteBillingRate,
} from '../types/customerTypes'

function toStringValue(
  value: string | null | undefined,
): string {
  return value ?? ''
}

function toNullableString(
  value: string | null | undefined,
): string | null {
  const normalized = value?.trim()

  return normalized
    ? normalized
    : null
}

function toNumberValue(
  value: number | null | undefined,
): number {
  return Number(value ?? 0)
}

function toNullableId(
  value: number | null | undefined,
): number | null {
  if (
    value == null
    || value <= 0
  ) {
    return null
  }

  return value
}

export function toCustomerSiteBillingRate(
  response: CustomerSiteBillingRateResponse,
): CustomerSiteBillingRate {
  return {
    id: response.id,
    customerSiteId:
      response.customerSiteId,

    deleteSelected: false,

    jobCode:
      toStringValue(response.jobCode),

    jobName:
      toStringValue(response.jobName),

    siteRoleCode:
      toStringValue(
        response.siteRoleCode,
      ) || 'GENERAL',

    siteRoleName:
      toStringValue(
        response.siteRoleName,
      ) || '一般',

    billingUnit:
      response.billingUnit ?? 'DAILY',

    baseUnitPrice:
      toNumberValue(
        response.baseUnitPrice,
      ),

    overtimeUnitPrice:
      toNumberValue(
        response.overtimeUnitPrice,
      ),

    nightUnitPrice:
      toNumberValue(
        response.nightUnitPrice,
      ),

    holidayUnitPrice:
      toNumberValue(
        response.holidayUnitPrice,
      ),

    commuteUnitPrice:
      toNumberValue(
        response.commuteUnitPrice,
      ),

    effectiveFrom:
      toStringValue(
        response.effectiveFrom,
      ),

    effectiveTo:
      response.effectiveTo ?? null,

    displayOrder:
      response.displayOrder ?? 1,

    activeFlag:
      response.activeFlag ?? true,

    note:
      toStringValue(response.note),

    _isNew: false,
    _isUpdated: false,
    _isDeleted: false,
  }
}

export function toCustomerSiteBillingRateRequest(
  row: CustomerSiteBillingRate,
): CustomerSiteBillingRateRequest {
  const customerSiteId =
    toNullableId(
      row.customerSiteId,
    )

  if (customerSiteId == null) {
    throw new Error(
      '現場は必須です。',
    )
  }

  return {
    id:
      toNullableId(row.id),

    customerSiteId,

    jobCode:
      row.jobCode.trim(),

    jobName:
      row.jobName.trim(),

    siteRoleCode:
      row.siteRoleCode.trim()
      || 'GENERAL',

    siteRoleName:
      row.siteRoleName.trim()
      || '一般',

    billingUnit:
      row.billingUnit ?? 'DAILY',

    baseUnitPrice:
      toNumberValue(
        row.baseUnitPrice,
      ),

    overtimeUnitPrice:
      toNumberValue(
        row.overtimeUnitPrice,
      ),

    nightUnitPrice:
      toNumberValue(
        row.nightUnitPrice,
      ),

    holidayUnitPrice:
      toNumberValue(
        row.holidayUnitPrice,
      ),

    commuteUnitPrice:
      toNumberValue(
        row.commuteUnitPrice,
      ),

    effectiveFrom:
      row.effectiveFrom,

    effectiveTo:
      toNullableString(
        row.effectiveTo,
      ),

    displayOrder:
      row.displayOrder ?? 1,

    activeFlag:
      row.activeFlag ?? true,

    note:
      toNullableString(row.note),

    _isNew:
      row._isNew,

    _isUpdated:
      row._isUpdated,

    _isDeleted:
      row._isDeleted,
  }
}

export function createEmptyCustomerSiteBillingRate(
  customerSiteId: number | null,
  displayOrder = 1,
): CustomerSiteBillingRate {
  return {
    id: -Date.now(),

    customerSiteId:
      customerSiteId ?? -1,

    deleteSelected: false,

    jobCode: '',
    jobName: '',

    siteRoleCode: 'GENERAL',
    siteRoleName: '一般',

    billingUnit: 'DAILY',

    baseUnitPrice: 0,
    overtimeUnitPrice: 0,
    nightUnitPrice: 0,
    holidayUnitPrice: 0,
    commuteUnitPrice: 0,

    effectiveFrom:
      new Date()
        .toISOString()
        .slice(0, 10),

    effectiveTo: null,

    displayOrder,
    activeFlag: true,

    note: '',

    _isNew: true,
    _isUpdated: false,
    _isDeleted: false,
  }
}