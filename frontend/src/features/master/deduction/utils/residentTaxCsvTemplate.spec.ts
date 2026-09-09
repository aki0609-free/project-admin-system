import { describe, expect, it } from 'vitest'
import {
  createResidentTaxCsvTemplate,
  resolveResidentTaxFiscalYear,
} from '@/features/master/deduction/utils/residentTaxCsvTemplate'
import type { ResidentTaxEditorResponse } from '@/features/master/deduction/types/residentTaxEditorTypes'

describe('residentTaxCsvTemplate', () => {
  it('参照基準日から6月始まりの住民税年度を求める', () => {
    expect(resolveResidentTaxFiscalYear('2026-06-01')).toBe(2026)
    expect(resolveResidentTaxFiscalYear('2027-05-31')).toBe(2026)
  })

  it('既存の住民税取込が読める12か月形式を出力する', () => {
    const response: ResidentTaxEditorResponse = {
      batchId: null,
      fiscalYear: 2026,
      status: 'NONE',
      hasClosedMonthChanges: false,
      employees: [
        {
          employeeId: 10,
          employeeCode: 'EMP-010',
          employeeName: '富陽, 太郎',
          months: [],
        },
      ],
    }

    const csv = createResidentTaxCsvTemplate(response, 2026)

    expect(csv).toContain('社員ID,社員コード,氏名,年度,6月,7月,8月,9月,10月,11月,12月,1月,2月,3月,4月,5月')
    expect(csv).toContain('10,EMP-010,"富陽, 太郎",2026')
    expect(csv.startsWith('\uFEFF')).toBe(true)
  })
})
