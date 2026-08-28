import { describe, expect, it } from 'vitest'
import type { ApplicationMediaLocalItem } from '@/features/application/types/applicationMediaTypes'
import { summarizeApplicationMedia } from './applicationMediaAnalytics'

function media(overrides: Partial<ApplicationMediaLocalItem>): ApplicationMediaLocalItem {
  return {
    id: 1,
    mediaName: '媒体A',
    mediaArea: '東京',
    mediaSlots: 1,
    mediaYearMonth: '2026-08',
    cost: 10000,
    hires: 2,
    unitPrice: 5000,
    isCreated: false,
    isUpdated: false,
    isDeleted: false,
    ...overrides,
  }
}

describe('summarizeApplicationMedia', () => {
  it('削除予定を除外して月別・媒体別・総計を一度に集計する', () => {
    const result = summarizeApplicationMedia([
      media({ id: 1 }),
      media({ id: 2, mediaYearMonth: '2026-09', cost: 5000, hires: 1 }),
      media({ id: 3, mediaName: '媒体B', cost: 9000, hires: 3 }),
      media({ id: 4, isDeleted: true, cost: 99999, hires: 99 }),
    ])

    expect(result.totalCost).toBe(24000)
    expect(result.totalHires).toBe(6)
    expect(result.averageUnitPrice).toBe(4000)
    expect(result.mediaSummary).toEqual([
      { mediaName: '媒体A', cost: 15000, hires: 3, unitPrice: 5000 },
      { mediaName: '媒体B', cost: 9000, hires: 3, unitPrice: 3000 },
    ])
    expect(result.monthlySummary).toHaveLength(2)
  })

  it('採用数0件では単価を0として返す', () => {
    const result = summarizeApplicationMedia([media({ hires: 0 })])
    expect(result.averageUnitPrice).toBe(0)
    expect(result.mediaSummary[0]?.unitPrice).toBe(0)
  })
})
