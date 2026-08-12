export type CustomerBillingClosing = {
  id: number
  targetMonth: string
  customerId: number
  status: 'OPEN' | 'CLOSED'
  closingVersion: number
  closedAt: string | null
}

export type CustomerBillingTarget = {
  customerId: number
  customerName: string
  closingRuleLabel: string
  periodFrom: string
  periodTo: string
  subtotalAmount: number
  taxAmount: number
  totalAmount: number
  calculationReady: boolean
  closingDateReached: boolean
  closing: CustomerBillingClosing | null
}

export type CustomerBillingSummary = {
  targetMonth: string
  status: 'OPEN' | 'PARTIALLY_CLOSED' | 'CLOSED' | 'TARGET_NONE'
  targetCount: number
  eligibleCount: number
  closedCount: number
  customers: CustomerBillingTarget[]
}

export type CustomerBillingBulkClosing = {
  targetCount: number
  completedCount: number
  skippedBeforeClosingDateCount: number
  alreadyClosedCount: number
  failedCount: number
  errors: string[]
}
