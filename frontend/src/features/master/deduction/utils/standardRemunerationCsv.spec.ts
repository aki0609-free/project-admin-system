import { describe, expect, it } from 'vitest'
import {
  createStandardRemunerationCsvTemplate,
  parseStandardRemunerationCsv,
} from './standardRemunerationCsv'
import type { StandardRemunerationEditorResponse } from '../types/standardRemunerationEditorTypes'

const editor: StandardRemunerationEditorResponse = {
  batchId: null,
  status: 'NONE',
  hasClosedPeriodChanges: false,
  employees: [{ employeeId: 1, employeeCode: 'E001', employeeName: '検証 太郎' }],
  rows: [{
    currentRecordId: 10,
    employeeId: 1,
    employeeCode: 'E001',
    employeeName: '検証 太郎',
    effectiveFrom: '2026-04-01',
    effectiveTo: null,
    healthStandardRemuneration: 220000,
    pensionStandardRemuneration: 220000,
    sourceType: 'REGULAR_DECISION',
    note: '定時決定',
    changed: false,
    closed: false,
  }],
}

describe('standardRemunerationCsv', () => {
  it('履歴をCSVへ出力して再取込できる', () => {
    const csv = createStandardRemunerationCsvTemplate(editor)
    const parsed = parseStandardRemunerationCsv(csv, editor)

    expect(parsed).toHaveLength(1)
    expect(parsed[0]).toMatchObject({
      currentRecordId: 10,
      employeeCode: 'E001',
      healthStandardRemuneration: 220000,
      pensionStandardRemuneration: 220000,
      effectiveFrom: '2026-04-01',
    })
  })

  it('存在しない社員コードを拒否する', () => {
    const csv = 'employeeCode,healthStandardRemuneration,pensionStandardRemuneration,effectiveFrom,effectiveTo,sourceType,note\nE999,220000,220000,2026-04-01,,MANUAL,'
    expect(() => parseStandardRemunerationCsv(csv, editor)).toThrow('社員コード')
  })

  it('既存履歴と未設定従業員を同じCSVテンプレートへ出力する', () => {
    const csv = createStandardRemunerationCsvTemplate({
      ...editor,
      employees: [
        ...editor.employees,
        { employeeId: 2, employeeCode: 'E002', employeeName: '未設定 花子' },
      ],
    })

    expect(csv).toContain('E001,220000,220000,2026-04-01,,REGULAR_DECISION')
    expect(csv).toContain('E002,,,,,MANUAL,')
  })
})
