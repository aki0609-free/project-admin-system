import { describe, expect, it } from 'vitest'

import { dailyReportAmountItemSchema } from './dailyReportSchema'

const amountItem = (overrides: Record<string, unknown> = {}) => ({
  masterId: 1,
  code: 'TEST_ITEM',
  name: 'テスト項目',
  itemType: 'DEDUCTION',
  inputMode: 'AUTO_WITH_OVERRIDE',
  calculatedAmount: 500,
  amount: 500,
  manualOverride: false,
  overrideReason: '',
  editable: true,
  displayOrder: 1,
  balanceTracked: false,
  balanceUnit: null,
  openingQuantity: 0,
  accruedQuantity: 0,
  consumedQuantity: 0,
  remainingQuantity: 0,
  quantity: 0,
  remainingAfterQuantity: 0,
  ...overrides,
})

describe('dailyReportAmountItemSchema', () => {
  it('負数の手動変更額を拒否する', () => {
    const result = dailyReportAmountItemSchema.safeParse(
      amountItem({ amount: -1, manualOverride: true, overrideReason: '調整' }),
    )

    expect(result.success).toBe(false)
  })

  it('手動変更時は変更理由を必須にする', () => {
    const result = dailyReportAmountItemSchema.safeParse(
      amountItem({ amount: 400, manualOverride: true, overrideReason: '  ' }),
    )

    expect(result.success).toBe(false)
  })
})
