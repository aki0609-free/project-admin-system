import type { SimpleTableEditableRow } from '@/shared/components/table/simple_table/types/item/types'
import type { DayRule } from '@/shared/types/dayRuleTypes'

export type CustomerPaymentStatus =
  | 'UNPAID'
  | 'PARTIAL'
  | 'PAID'
  | 'OVERPAID'
  | 'CANCELED'
  | 'WRITE_OFF'

export type CustomerBillingUnit =
  | 'HOURLY'
  | 'DAILY'
  | 'MONTHLY'

export type CustomerInvoiceType =
  | 'PATTERN_1'
  | 'PATTERN_2'
  | 'PATTERN_3'

export type CustomerContractStatus =
  | 'ACTIVE'
  | 'INACTIVE'
  | 'ENDED'

export type Customer = {
  id: number
  name: string
  furiganaName: string
  shortName: string
  postNo: string
  address: string
  representativeName: string
  phone: string
  jobType: string
  contractFlag: CustomerContractStatus
  invoiceType: CustomerInvoiceType
  closingDayRule: DayRule | null
  paymentDayRule: DayRule | null
}

export type CustomerListItem = Customer & {
  siteCount: number
  employeeCount: number
  latestPaymentStatus: string
}

export type CustomerSite = SimpleTableEditableRow & {
  customerId: number
  deleteSelected?: boolean
  name: string
  contactPersonName: string
  contactPersonPhone: string
  contactPersonEmail: string
  distanceFromCompanyKm: number | null
}

export type CustomerSiteBillingRate =
  SimpleTableEditableRow & {
    customerSiteId: number

    deleteSelected?: boolean

    jobCode: string
    jobName: string

    siteRoleCode: string
    siteRoleName: string

    billingUnit: CustomerBillingUnit

    baseUnitPrice: number
    overtimeUnitPrice: number
    nightUnitPrice: number

    // ★追加
    holidayUnitPrice: number

    commuteUnitPrice: number

    effectiveFrom: string
    effectiveTo: string | null

    displayOrder: number
    activeFlag: boolean
    note: string
  }

export type CustomerEmployee = SimpleTableEditableRow & {
  customerId: number
  deleteSelected?: boolean
  name: string
  furiganaName: string
  position: string
  phone: string
  email: string
  invoiceToFlag: boolean
  invoiceCcFlag: boolean
}

export type CustomerSavePayload = {
  customer: Customer
  sites: CustomerSite[]
  employees: CustomerEmployee[]
}

export type CustomerTransactionSavePayload = CustomerTransaction

export type CustomerTransaction = SimpleTableEditableRow & {
  customerId: number
  customerName?: string
  deleteSelected?: boolean
  targetMonth: string
  closingDayRule: DayRule | null
  paymentDayRule: DayRule | null
  billingAmount: number | null
  expectedPaymentDate: string | null
  confirmedPaymentDate: string | null
  paidAmount: number | null
  fee: number | null
  offsetAmount: number | null
  adjustmentAmount: number | null
  totalAmount: number | null
  remainingAmount: number
  paymentStatus: CustomerPaymentStatus
  note: string
}

export type CustomerPaymentConfirmPayload = {
  confirmedPaymentDate: string | null
  paidAmount: number | null
  fee: number | null
  offsetAmount: number | null
  adjustmentAmount: number | null
  note: string | null
}
