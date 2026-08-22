import { describe, expect, it } from 'vitest'

import {
  getCalendarMonthRange,
  moveCalendarMonth,
} from './dashboardDate'

describe('dashboardDate', () => {
  it('月末から翌月へ移動しても月を飛び越さない', () => {
    expect(moveCalendarMonth('2026-01-31', 1)).toBe('2026-02-01')
    expect(moveCalendarMonth('2026-03-31', -1)).toBe('2026-02-01')
  })

  it('うるう年を含めて表示月の月初と月末を返す', () => {
    expect(getCalendarMonthRange('2028-02-15')).toEqual({
      from: '2028-02-01',
      to: '2028-02-29',
    })
    expect(getCalendarMonthRange('2027-02-15')).toEqual({
      from: '2027-02-01',
      to: '2027-02-28',
    })
  })
})
