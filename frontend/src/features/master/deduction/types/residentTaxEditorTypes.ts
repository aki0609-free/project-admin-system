export type ResidentTaxMonthEditor = {
  month: number
  currentTaxAmount: number | null
  draftTaxAmount: number | null
  changed: boolean
  closed: boolean
}

export type ResidentTaxEmployeeEditor = {
  employeeId: number
  employeeCode: string
  employeeName: string
  months: ResidentTaxMonthEditor[]
}

export type ResidentTaxEditorResponse = {
  batchId: number | null
  fiscalYear: number
  status: 'NONE' | 'DRAFT' | 'VALIDATED' | 'CONFIRMED'
  hasClosedMonthChanges: boolean
  employees: ResidentTaxEmployeeEditor[]
}

export type ResidentTaxDraftSaveRequest = {
  fiscalYear: number
  employees: Array<{
    employeeId: number
    months: Array<{ month: number; taxAmount: number | null }>
  }>
}

export type ResidentTaxConfirmRequest = {
  changeReason: string
  acknowledgeReclosing: boolean
}
