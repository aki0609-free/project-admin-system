import { describe, expect, it } from 'vitest'

import { createEmptyDailyReportForm } from './dailyReportFormFactory'

describe('dailyReportFormFactory', () => {
  it('新規日報の開始・終了時刻へ標準時刻を設定する', () => {
    const form = createEmptyDailyReportForm()

    expect(form.startTime).toBe('08:00')
    expect(form.endTime).toBe('17:00')
    expect(form.savingWithdrawalAmount).toBe(0)
  })
})
