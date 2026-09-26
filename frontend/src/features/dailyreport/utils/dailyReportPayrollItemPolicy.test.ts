import { describe, expect, it } from 'vitest'

import {
  LEGAL_DEPOSIT_SYSTEM_OVERRIDE_REASON,
  requiresManualOverrideReason,
  resolveManualOverrideReason,
} from './dailyReportPayrollItemPolicy'

describe('dailyReportPayrollItemPolicy', () => {
  it('法定準備金は理由欄を表示せずシステム理由を保存する', () => {
    expect(
      requiresManualOverrideReason('LEGAL_DEPOSIT', true),
    ).toBe(false)
    expect(
      resolveManualOverrideReason('LEGAL_DEPOSIT', true, ''),
    ).toBe(LEGAL_DEPOSIT_SYSTEM_OVERRIDE_REASON)
  })

  it('その他の手動変更は入力理由を維持する', () => {
    expect(
      requiresManualOverrideReason('OTHER_DEDUCTION', true),
    ).toBe(true)
    expect(
      resolveManualOverrideReason(
        'OTHER_DEDUCTION',
        true,
        ' 金額調整 ',
      ),
    ).toBe('金額調整')
  })
})
