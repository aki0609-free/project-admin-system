export type StandardRemunerationEmployeeOption = {
  employeeId: number
  employeeCode: string
  employeeName: string
}

export type StandardRemunerationEditorRow = {
  currentRecordId: number | null
  employeeId: number
  employeeCode: string
  employeeName: string
  effectiveFrom: string
  effectiveTo: string | null
  healthStandardRemuneration: number
  pensionStandardRemuneration: number
  sourceType: string
  note: string | null
  changed: boolean
  closed: boolean
}

export type StandardRemunerationEditorResponse = {
  batchId: number | null
  status: 'NONE' | 'DRAFT' | 'VALIDATED' | 'CONFIRMED'
  hasClosedPeriodChanges: boolean
  employees: StandardRemunerationEmployeeOption[]
  rows: StandardRemunerationEditorRow[]
}

export type StandardRemunerationDraftSaveRequest = {
  rows: Array<{
    currentRecordId: number | null
    employeeId: number
    effectiveFrom: string
    effectiveTo: string | null
    healthStandardRemuneration: number
    pensionStandardRemuneration: number
    sourceType: string
    note: string | null
  }>
}

export type StandardRemunerationConfirmRequest = {
  changeReason: string
  acknowledgeReclosing: boolean
}
