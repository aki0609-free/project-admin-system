import { describe, expect, it } from 'vitest'

import { formatDateInTimeZone } from './DateUtils'

describe('formatDateInTimeZone', () => {
  it('UTCでは前日でも日本時間の日付を返す', () => {
    const earlyMorningInJapan =
      new Date('2026-09-05T16:30:00.000Z')

    expect(
      formatDateInTimeZone(earlyMorningInJapan),
    ).toBe('2026-09-06')
  })
})
