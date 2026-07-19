import type { DayRule } from '../types/dayRuleTypes'

function formatMonthOffset(monthOffset?: number | null): string {
  switch (monthOffset ?? 0) {
    case 0:
      return '当月'
    case 1:
      return '翌月'
    case 2:
      return '翌々月'
    default:
      return `${monthOffset}か月後`
  }
}

export function formatDayRule(rule?: DayRule | null): string {
  if (!rule || !rule.type) return ''

  const monthLabel = formatMonthOffset(rule.monthOffset)

  switch (rule.type) {
    case 'DAY_OF_MONTH':
      return `${monthLabel}${rule.value ?? ''}日`

    case 'END_OF_MONTH':
      return `${monthLabel}末日`

    case 'EXACT_DAY':
      return '当日'

    case 'BEFORE_DAYS':
      return `${rule.value ?? 0}日前`

    case 'AFTER_DAYS':
      return `${rule.value ?? 0}日後`

    default:
      return ''
  }
}