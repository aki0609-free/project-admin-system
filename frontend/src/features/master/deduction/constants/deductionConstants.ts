import type {
  DeductionCalculationType,
  DeductionDetailViewType,
  DeductionType,
  DeductionUnit,
} from '@/features/master/deduction/types/deductionTypes'

export const deductionTypeLabelMap: Record<DeductionType, string> = {
  LEGAL: '法定控除',
  COMPANY: '会社控除',
  EMPLOYEE: '従業員控除',
  TEMPORARY: '一時控除',
  ADJUSTMENT: '調整',
  OTHER: 'その他',
}

export const deductionCalculationTypeLabelMap: Record<DeductionCalculationType, string> = {
  MANUAL: '手入力',
  FIXED: '固定',
  AUTO: '自動計算',
}

export const deductionUnitLabelMap: Record<DeductionUnit, string> = {
  DAILY: '日単位',
  MONTHLY: '月単位',
  BOTH: '日・月両方',
  PAYROLL: '給与計算時',
}

export const deductionDetailViewTypeLabelMap: Record<DeductionDetailViewType, string> = {
  NONE: '詳細なし',
  INCOME_TAX: '所得税',
  RESIDENT_TAX: '住民税',
  HEALTH_INSURANCE: '健康保険',
  PENSION: '厚生年金',
  EMPLOYMENT_INSURANCE: '雇用保険',
}

export const deductionTypeOptions = Object.entries(
  deductionTypeLabelMap,
).map(([value, title]) => ({
  value,
  title,
}))

export const deductionCalculationTypeOptions = Object.entries(
  deductionCalculationTypeLabelMap,
).map(([value, title]) => ({
  value,
  title,
}))

export const deductionUnitOptions = Object.entries(
  deductionUnitLabelMap,
).map(([value, title]) => ({
  value,
  title,
}))

export const deductionDetailViewTypeOptions = Object.entries(
  deductionDetailViewTypeLabelMap,
).map(([value, title]) => ({
  value,
  title,
}))