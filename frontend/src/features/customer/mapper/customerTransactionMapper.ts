import type { DayRule } from '@/shared/types/dayRuleTypes'
import type {
  CustomerPaymentConfirmRequest,
  CustomerTransactionRequest,
  CustomerTransactionResponse,
} from '../types/customerApiTypes'
import type { CustomerPaymentConfirmPayload, CustomerTransaction } from '../types/customerTypes'

function toStringValue(value: string | null | undefined): string {
  return value ?? ''
}

function toNullableString(value: string | null | undefined): string | null {
  if (value == null || value === '') return null
  return value
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
    value: value.type === 'END_OF_MONTH' ? null : (value.value ?? null),
    monthOffset: value.monthOffset ?? 0,
  }
}

export function toCustomerTransaction(response: CustomerTransactionResponse): CustomerTransaction {
  return {
    id: response.id,
    customerId: response.customerId,
    deleteSelected: false,
    targetMonth: toStringValue(response.targetMonth),
    closingDayRule: toDayRule(response.closingDayRule),
    paymentDayRule: toDayRule(response.paymentDayRule),
    billingAmount: response.billingAmount,
    expectedPaymentDate: response.expectedPaymentDate,
    confirmedPaymentDate: response.confirmedPaymentDate,
    paidAmount: response.paidAmount,
    fee: response.fee,
    receivableAmount: (response.billingAmount ?? 0) + (response.fee ?? 0),
    offsetAmount: response.offsetAmount,
    totalAmount: response.totalAmount,
    paymentStatus: response.paymentStatus ?? 'UNPAID',
    note: toStringValue(response.note),
    _isNew: false,
    _isUpdated: false,
    _isDeleted: false,
  }
}

export function toCustomerTransactionRequest(row: CustomerTransaction): CustomerTransactionRequest {
  return {
    targetMonth: toNullableString(row.targetMonth),
    closingDayRule: toDayRuleRequest(row.closingDayRule),
    paymentDayRule: toDayRuleRequest(row.paymentDayRule),
    billingAmount: row.billingAmount,
    expectedPaymentDate: row.expectedPaymentDate,
    confirmedPaymentDate: row.confirmedPaymentDate,
    paidAmount: row.paidAmount,
    fee: row.fee,
    offsetAmount: row.offsetAmount,
    totalAmount: row.totalAmount,
    paymentStatus: row.paymentStatus,
    note: toNullableString(row.note),
  }
}

export function toCustomerPaymentConfirmRequest(
  payload: CustomerPaymentConfirmPayload,
): CustomerPaymentConfirmRequest {
  return {
    confirmedPaymentDate: payload.confirmedPaymentDate,
    paidAmount: payload.paidAmount,
    fee: payload.fee,
    offsetAmount: payload.offsetAmount,
    note: toNullableString(payload.note ?? null),
  }
}
