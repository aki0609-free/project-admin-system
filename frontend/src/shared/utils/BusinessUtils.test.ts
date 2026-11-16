import { describe, expect, it } from 'vitest'

import { calculateAgeAt, calculateCurrentAge } from './BusinessUtils'

describe('calculateCurrentAge', () => {
  const currentDate = new Date(2026, 10, 13, 12)

  it('誕生日前は前年齢を返す', () => {
    expect(calculateCurrentAge('1990-11-14', currentDate)).toBe('35')
  })

  it('誕生日当日から新しい年齢を返す', () => {
    expect(calculateCurrentAge('1990-11-13', currentDate)).toBe('36')
  })

  it('誕生日後は新しい年齢を返す', () => {
    expect(calculateCurrentAge('1990-11-12', currentDate)).toBe('36')
  })

  it('生年月日未登録時は空欄を返す', () => {
    expect(calculateCurrentAge(null, currentDate)).toBe('')
  })
})

describe('calculateAgeAt', () => {
  it('不正な日付は空欄を返す', () => {
    expect(calculateAgeAt('1990-02-31', '2026-11-13')).toBe('')
  })
})
