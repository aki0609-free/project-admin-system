import type {
  CustomerDetailResponse,
  CustomerEmployeeRequest,
  CustomerEmployeeResponse,
  CustomerListItemResponse,
  CustomerSaveRequest,
  CustomerSiteRequest,
  CustomerSiteResponse,
} from '../types/customerApiTypes'
import type {
  Customer,
  CustomerEmployee,
  CustomerListItem,
  CustomerSavePayload,
  CustomerSite,
} from '../types/customerTypes'
import type { DayRule } from '@/shared/types/dayRuleTypes'

function toStringValue(value: string | null | undefined): string {
  return value ?? ''
}

function toNullableString(value: string | null | undefined): string | null {
  if (value == null || value === '') return null
  return value
}

function toNullableId(id: number | null | undefined): number | null {
  if (id == null || id < 0) return null
  return id
}

function toDayRule(value: DayRule | null | undefined): DayRule | null {
  if (!value || !value.type) return null

  return {
    type: value.type,
    value: value.value ?? null,
    monthOffset: value.monthOffset ?? 0,
    label: value.label,
  }
}

function toDayRuleRequest(value: DayRule | null | undefined): DayRule | null {
  if (!value || !value.type) return null

  return {
    type: value.type,
    value: value.type === 'END_OF_MONTH'
      ? null
      : value.value ?? null,
    monthOffset: value.monthOffset ?? 0,
  }
}

export function toCustomer(
  response: CustomerListItemResponse | CustomerDetailResponse,
): Customer {
  return {
    id: response.id,
    name: response.name,
    furiganaName: toStringValue(response.furiganaName),
    shortName: toStringValue(response.shortName),
    postNo: toStringValue(response.postNo),
    address: toStringValue(response.address),
    representativeName: toStringValue(response.representativeName),
    phone: toStringValue(response.phone),
    jobType: toStringValue(response.jobType),
    contractFlag: toStringValue(response.contractFlag),
    closingDayRule: toDayRule(response.closingDayRule),
    paymentDayRule: toDayRule(response.paymentDayRule),
  }
}

export function toCustomerListItem(
  response: CustomerListItemResponse,
): CustomerListItem {
  return {
    ...toCustomer(response),
    siteCount: response.siteCount ?? 0,
    employeeCount: response.employeeCount ?? 0,
    latestPaymentStatus: response.latestPaymentStatus ?? '未',
  }
}

export function toCustomerSite(
  response: CustomerSiteResponse,
): CustomerSite {
  return {
    id: response.id,
    customerId: response.customerId,
    deleteSelected: false,
    name: toStringValue(response.name),
    contactPersonName: toStringValue(response.contactPersonName),
    contactPersonPhone: toStringValue(response.contactPersonPhone),
    contactPersonEmail: toStringValue(response.contactPersonEmail),
    distanceFromCompanyKm: response.distanceFromCompanyKm,
    _isNew: false,
    _isUpdated: false,
    _isDeleted: false,
  }
}

export function toCustomerEmployee(
  response: CustomerEmployeeResponse,
): CustomerEmployee {
  return {
    id: response.id,
    customerId: response.customerId,
    deleteSelected: false,
    name: toStringValue(response.name),
    furiganaName: toStringValue(response.furiganaName),
    position: toStringValue(response.position),
    phone: toStringValue(response.phone),
    email: toStringValue(response.email),
    invoiceToFlag: response.invoiceToFlag ?? false,
    invoiceCcFlag: response.invoiceCcFlag ?? false,
    _isNew: false,
    _isUpdated: false,
    _isDeleted: false,
  }
}

function toCustomerSiteRequest(
  row: CustomerSite,
): CustomerSiteRequest {
  return {
    id: toNullableId(row.id),
    name: toNullableString(row.name),
    contactPersonName: toNullableString(row.contactPersonName),
    contactPersonPhone: toNullableString(row.contactPersonPhone),
    contactPersonEmail: toNullableString(row.contactPersonEmail),
    distanceFromCompanyKm: row.distanceFromCompanyKm,
    _isNew: row._isNew,
    _isUpdated: row._isUpdated,
    _isDeleted: row._isDeleted,
  }
}

function toCustomerEmployeeRequest(
  row: CustomerEmployee,
): CustomerEmployeeRequest {
  return {
    id: toNullableId(row.id),
    name: toNullableString(row.name),
    furiganaName: toNullableString(row.furiganaName),
    position: toNullableString(row.position),
    phone: toNullableString(row.phone),
    email: toNullableString(row.email),
    invoiceToFlag: row.invoiceToFlag,
    invoiceCcFlag: row.invoiceCcFlag,
    _isNew: row._isNew,
    _isUpdated: row._isUpdated,
    _isDeleted: row._isDeleted,
  }
}

export function toCustomerSaveRequest(
  payload: CustomerSavePayload,
): CustomerSaveRequest {
  return {
    name: payload.customer.name,
    furiganaName: toNullableString(payload.customer.furiganaName),
    shortName: toNullableString(payload.customer.shortName),
    postNo: toNullableString(payload.customer.postNo),
    address: toNullableString(payload.customer.address),
    representativeName: toNullableString(payload.customer.representativeName),
    phone: toNullableString(payload.customer.phone),
    jobType: toNullableString(payload.customer.jobType),
    contractFlag: toNullableString(payload.customer.contractFlag),
    closingDayRule: toDayRuleRequest(payload.customer.closingDayRule),
    paymentDayRule: toDayRuleRequest(payload.customer.paymentDayRule),
    sites: payload.sites.map(toCustomerSiteRequest),
    employees: payload.employees.map(toCustomerEmployeeRequest),
  }
}