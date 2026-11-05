export type YearEndAdjustment = {
  id: number | null
  employeeId: number | null
  employeeCode?: string
  employeeName?: string
  taxYear: number
  targetMonth: string
  adjustmentAmount: number
  status: 'DRAFT' | 'CONFIRMED'
  note: string | null
  lockVersion: number | null
}
