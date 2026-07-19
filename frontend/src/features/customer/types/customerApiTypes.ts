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
  | 'FIXED'

export type CustomerSiteResponse = {
  id: number
  customerId: number
  name: string | null
  contactPersonName: string | null
  contactPersonPhone: string | null
  contactPersonEmail: string | null
  distanceFromCompanyKm: number | null
}

export type CustomerSiteBillingRateResponse = {
  id: number
  customerSiteId: number

  jobCode: string
  jobName: string

  siteRoleCode: string
  siteRoleName: string

  billingUnit: CustomerBillingUnit

  baseUnitPrice: number
  overtimeUnitPrice: number
  nightUnitPrice: number
  holidayUnitPrice: number
  commuteUnitPrice: number

  effectiveFrom: string
  effectiveTo: string | null

  displayOrder: number
  activeFlag: boolean
  note: string | null
}

export type CustomerSiteBillingRateRequest = {
  id: number | null

  customerSiteId: number

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
  note: string | null

  _isNew?: boolean
  _isUpdated?: boolean
  _isDeleted?: boolean
}

export type CustomerEmployeeResponse = {
  id: number
  customerId: number
  name: string | null
  furiganaName: string | null
  position: string | null
  phone: string | null
  email: string | null
  invoiceToFlag: boolean | null
  invoiceCcFlag: boolean | null
}

export type CustomerListItemResponse = {
  id: number
  name: string
  furiganaName: string | null
  shortName: string | null
  postNo: string | null
  address: string | null
  representativeName: string | null
  phone: string | null
  jobType: string | null
  contractFlag: string | null
  closingDayRule: DayRule | null
  paymentDayRule: DayRule | null
  siteCount: number | null
  employeeCount: number | null
  latestPaymentStatus: string | null
}

export type CustomerDetailResponse = CustomerListItemResponse & {
  sites: CustomerSiteResponse[]
  employees: CustomerEmployeeResponse[]
}

export type CustomerSiteRequest = {
  id: number | null
  name: string | null
  contactPersonName: string | null
  contactPersonPhone: string | null
  contactPersonEmail: string | null
  distanceFromCompanyKm: number | null
  _isNew?: boolean
  _isUpdated?: boolean
  _isDeleted?: boolean
}

export type CustomerEmployeeRequest = {
  id: number | null
  name: string | null
  furiganaName: string | null
  position: string | null
  phone: string | null
  email: string | null
  invoiceToFlag: boolean
  invoiceCcFlag: boolean
  _isNew?: boolean
  _isUpdated?: boolean
  _isDeleted?: boolean
}

export type CustomerSaveRequest = {
  name: string
  furiganaName: string | null
  shortName: string | null
  postNo: string | null
  address: string | null
  representativeName: string | null
  phone: string | null
  jobType: string | null
  contractFlag: string | null
  closingDayRule: DayRule | null
  paymentDayRule: DayRule | null
  sites: CustomerSiteRequest[]
  employees: CustomerEmployeeRequest[]
}

export type CustomerOptionItemResponse = {
  id: number
  name: string
}

export type CustomerSiteOptionItemResponse = {
  id: number
  customerId: number
  name: string
  distanceFromCompanyKm: number | null
}

export type CustomerOptionResponse = {
  customers: CustomerOptionItemResponse[]
  sites: CustomerSiteOptionItemResponse[]
}

export type CustomerPaymentConfirmRequest = {
  confirmedPaymentDate: string | null
  paidAmount: number | null
  fee: number | null
  offsetAmount: number | null
  note: string | null
}

export type CustomerTransactionResponse = {
  id: number
  customerId: number
  targetMonth: string | null
  closingDayRule: DayRule | null
  paymentDayRule: DayRule | null
  billingAmount: number | null
  expectedPaymentDate: string | null
  confirmedPaymentDate: string | null
  paidAmount: number | null
  fee: number | null
  offsetAmount: number | null
  totalAmount: number | null
  paymentStatus: CustomerPaymentStatus | null
  note: string | null
}

export type CustomerTransactionRequest = {
  targetMonth: string | null
  closingDayRule: DayRule | null
  paymentDayRule: DayRule | null
  billingAmount: number | null
  expectedPaymentDate: string | null
  confirmedPaymentDate: string | null
  paidAmount: number | null
  fee: number | null
  offsetAmount: number | null
  totalAmount: number | null
  paymentStatus: CustomerPaymentStatus
  note: string | null
}