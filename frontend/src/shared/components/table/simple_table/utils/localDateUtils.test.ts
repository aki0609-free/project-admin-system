import { describe, expect, it } from 'vitest'

import { normalizeSimpleTableDateValue } from './localDateUtils'

describe('normalizeSimpleTableDateValue', () => {
  it('過去日のDateをAPI用のローカル日付文字列へ正規化する', () => {
    expect(
      normalizeSimpleTableDateValue(
        new Date(2024, 2, 31),
      ),
    ).toBe('2024-03-31')
  })

  it('既存のISO日付文字列を変更しない', () => {
    expect(
      normalizeSimpleTableDateValue('2025-01-01'),
    ).toBe('2025-01-01')
  })
})
