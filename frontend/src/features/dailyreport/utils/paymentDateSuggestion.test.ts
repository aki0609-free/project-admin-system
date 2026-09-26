import { describe, expect, it } from 'vitest'

import {
  calculateDefaultWorkDate,
  calculateSuggestedPaymentDate,
} from './paymentDateSuggestion'

describe('calculateSuggestedPaymentDate', () => {
  it('勤務日の前日が日曜日なら土曜日を初期値にする', () => {
    expect(calculateDefaultWorkDate('2026-07-20'))
      .toBe('2026-07-18')
    expect(calculateDefaultWorkDate('2026-07-21'))
      .toBe('2026-07-20')
  })

  it('日払いは翌日を提案する', () => {
    expect(calculateSuggestedPaymentDate('2026-07-16', 'DAILY'))
      .toBe('2026-07-17')
  })

  it('土曜日は通常の支払日として扱う', () => {
    expect(calculateSuggestedPaymentDate('2026-07-17', 'DAILY'))
      .toBe('2026-07-18')
  })

  it('日曜日だけを後ろ倒しし、祝日は通常日として扱う', () => {
    expect(calculateSuggestedPaymentDate('2026-07-18', 'DAILY'))
      .toBe('2026-07-20')
  })

  it('週払いは祝日でも翌週月曜日を提案する', () => {
    expect(calculateSuggestedPaymentDate('2026-07-13', 'WEEKLY'))
      .toBe('2026-07-20')
  })

  it('月払いは翌月15日が土曜日でもその日を提案する', () => {
    expect(calculateSuggestedPaymentDate('2026-07-10', 'MONTHLY'))
      .toBe('2026-08-15')
  })

  it('月払いの翌月15日が日曜日なら月曜日へ送る', () => {
    expect(calculateSuggestedPaymentDate('2026-10-10', 'MONTHLY'))
      .toBe('2026-11-16')
  })

  it('不正な日付または支払区分未設定では提案しない', () => {
    expect(calculateSuggestedPaymentDate('2026-02-30', 'DAILY'))
      .toBe('')
    expect(calculateSuggestedPaymentDate('2026-07-10', null))
      .toBe('')
  })
})
