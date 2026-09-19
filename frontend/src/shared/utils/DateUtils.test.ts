import { describe, expect, it } from 'vitest'

import {
  businessDateWithOffset,
  businessMonthWithOffset,
  formatDateInTimeZone,
} from './DateUtils'

describe('formatDateInTimeZone', () => {
  it('UTCでは前日でも日本時間の日付を返す', () => {
    const earlyMorningInJapan =
      new Date('2026-09-05T16:30:00.000Z')

    expect(
      formatDateInTimeZone(earlyMorningInJapan),
    ).toBe('2026-09-06')
  })
})

describe('businessDateWithOffset', () => {
  it('日本時間の業務日を1日前へ移動する', () => {
    const septemberFirstInJapan =
      new Date('2026-08-31T15:30:00.000Z')

    expect(
      businessDateWithOffset(-1, septemberFirstInJapan),
    ).toBe('2026-08-31')
    expect(
      businessMonthWithOffset(-1, septemberFirstInJapan),
    ).toBe('2026-08')
  })

  it('現場配置・配車の日付初期値を算出できる', () => {
    const septemberFirstInJapan =
      new Date('2026-08-31T15:30:00.000Z')

    expect(
      businessDateWithOffset(-1, septemberFirstInJapan),
    ).toBe('2026-08-31')
  })
})
