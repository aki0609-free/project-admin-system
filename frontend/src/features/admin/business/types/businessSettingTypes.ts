import type { DayRule } from '@/shared/types/dayRuleTypes'
import type { DormitoryType } from '@/features/employees/types/employeeApiTypes'

export type ResignationMessage = {
  dialogTitle: string
  guidanceMessage: string
  confirmationMessage: string
}

export type ResignationChecklistItem = {
  id: number
  code: string
  name: string
  description: string | null
  requiredFlag: boolean
  displayOrder: number
  activeFlag: boolean
}

export type ResignationChecklistSaveRequest = Omit<ResignationChecklistItem, 'id'>

export type BusinessClosingSetting = {
  id: number | null
  settingCode: string
  closingDay: DayRule
  paymentDay: DayRule
  activeFlag: boolean
}

export type MonthlyClosingOutputSetting = {
  id: number | null
  reportCode: string
  reportName: string | null
  jobCode: string | null
  outputType:
    | 'NONE'
    | 'HTML_PREVIEW'
    | 'HTML_PRINT'
    | 'PDF'
    | 'CSV'
    | 'EXCEL'
    | 'EXCEL_BOOK'
    | 'CUSTOM'
  executionOrder: number
  requiredFlag: boolean
  activeFlag: boolean
  backupRetentionYears: number | null
}

export type MonthlyClosingOutputSaveRequest = Pick<
  MonthlyClosingOutputSetting,
  'reportCode' | 'executionOrder' | 'activeFlag' | 'backupRetentionYears'
>

export type DormitoryFeeSetting = {
  id: number | null
  dormitoryType: DormitoryType
  dailyAmount: number
  activeFlag: boolean
}

export type DormitoryFeeSettingSaveRequest = Pick<
  DormitoryFeeSetting,
  'dormitoryType' | 'dailyAmount' | 'activeFlag'
>
