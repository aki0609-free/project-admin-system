// src/shared/types/dayRuleTypes.ts

export type DayRuleType = 
  | 'BEFORE_DAYS'
  | 'EXACT_DAY'
  | 'DAY_OF_MONTH'
  | 'END_OF_MONTH'
  | 'AFTER_DAYS'

export type DayRule = {
  type: DayRuleType
  value: number | null
  monthOffset: number
  label?: string
}