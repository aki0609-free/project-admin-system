import { describe, expect, it } from 'vitest'
import { defaultSimpleTablePredicates } from './defaultSimpleTablePredicates'

describe('defaultSimpleTablePredicates.number', () => {
  const predicate = defaultSimpleTablePredicates.number<{ vehicleCount: number }>(
    (item) => item.vehicleCount,
  )

  it('数値入力とブラウザから渡される文字列入力の両方で完全一致する', () => {
    expect(predicate({ vehicleCount: 2 }, 2)).toBe(true)
    expect(predicate({ vehicleCount: 2 }, '2')).toBe(true)
    expect(predicate({ vehicleCount: 12 }, '2')).toBe(false)
  })

  it('未入力なら全件を対象にする', () => {
    expect(predicate({ vehicleCount: 2 }, '')).toBe(true)
    expect(predicate({ vehicleCount: 2 }, null)).toBe(true)
  })
})
