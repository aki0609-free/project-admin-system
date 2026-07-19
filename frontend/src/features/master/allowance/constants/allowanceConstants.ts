import type {
  AllowanceCalculationType,
  AllowanceDetailViewType,
  AllowanceType,
  AllowanceUnit,
} from '@/features/master/allowance/types/allowanceTypes'

export const allowanceTypeLabelMap: Record<AllowanceType, string> = {
  LEGAL: '法定手当',
  COMPANY: '会社手当',
  EMPLOYEE: '従業員手当',
  TEMPORARY: '一時手当',
  ADJUSTMENT: '調整',
  OTHER: 'その他',
}

export const allowanceCalculationTypeLabelMap: Record<AllowanceCalculationType, string> = {
  MANUAL: '手入力',
  FIXED: '固定',
  AUTO: '自動計算',
}

export const allowanceUnitLabelMap: Record<AllowanceUnit, string> = {
  DAILY: '日単位',
  MONTHLY: '月単位',
  BOTH: '日・月両方',
  PAYROLL: '給与計算時',
}

export const allowanceDetailViewTypeLabelMap: Record<AllowanceDetailViewType, string> = {
  NONE: '詳細なし',
}

export const allowanceTypeOptions = Object.entries(
  allowanceTypeLabelMap,
).map(([value, title]) => ({
  value,
  title,
}))

export const allowanceCalculationTypeOptions = Object.entries(
  allowanceCalculationTypeLabelMap,
).map(([value, title]) => ({
  value,
  title,
}))

export const allowanceUnitOptions = Object.entries(
  allowanceUnitLabelMap,
).map(([value, title]) => ({
  value,
  title,
}))

export const allowanceDetailViewTypeOptions = Object.entries(
  allowanceDetailViewTypeLabelMap,
).map(([value, title]) => ({
  value,
  title,
}))