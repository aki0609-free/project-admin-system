import { describe, expect, it } from 'vitest'

import {
  calculateDefaultWorkDate,
  calculateSuggestedPaymentDate,
} from './paymentDateSuggestion'

describe('calculateSuggestedPaymentDate', () => {
  it('勤務日は日曜を避けるが土曜日は通常日とする', () => {
    expect(calculateDefaultWorkDate('2026-07-20'))
      .toBe('2026-07-18')
    expect(calculateDefaultWorkDate('2026-07-21'))
      .toBe('2026-07-20')
  })

  it('勤務日はお盆を避けた直前の日を初期値にする', () => {
    expect(calculateDefaultWorkDate('2026-08-16'))
      .toBe('2026-08-12')
  })

  it('日払いは翌日を提案する', () => {
    expect(calculateSuggestedPaymentDate('2026-07-16', 'DAILY'))
      .toBe('2026-07-17')
  })

  it('日払いの支払日が土曜日ならそのままにする', () => {
    expect(calculateSuggestedPaymentDate('2026-07-17', 'DAILY'))
      .toBe('2026-07-18')
  })

  it('日曜日は月曜日へ後ろ倒しする', () => {
    expect(calculateSuggestedPaymentDate('2026-07-18', 'DAILY'))
      .toBe('2026-07-20')
  })

  it('お盆期間は終了後へ後ろ倒しする', () => {
    expect(calculateSuggestedPaymentDate('2026-08-12', 'DAILY'))
      .toBe('2026-08-17')
  })

  it('週払いは祝日でも翌週月曜日を提案する', () => {
    expect(calculateSuggestedPaymentDate('2026-07-13', 'WEEKLY'))
      .toBe('2026-07-20')
  })

  it('月払いの翌月15日がお盆かつ土曜日ならお盆前へ前倒しする', () => {
    expect(calculateSuggestedPaymentDate('2026-07-10', 'MONTHLY'))
      .toBe('2026-08-12')
  })

  it('月払いの翌月15日が日曜日なら金曜日へ前倒しする', () => {
    expect(calculateSuggestedPaymentDate('2026-10-10', 'MONTHLY'))
      .toBe('2026-11-13')
  })

  it('月払いの翌月15日が土曜日なら金曜日へ前倒しする', () => {
    expect(calculateSuggestedPaymentDate('2027-04-10', 'MONTHLY'))
      .toBe('2027-05-14')
  })

  it('不正な日付または支払区分未設定では提案しない', () => {
    expect(calculateSuggestedPaymentDate('2026-02-30', 'DAILY'))
      .toBe('')
    expect(calculateSuggestedPaymentDate('2026-07-10', null))
      .toBe('')
  })
})
