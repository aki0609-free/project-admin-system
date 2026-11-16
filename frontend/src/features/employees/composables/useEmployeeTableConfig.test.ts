import { ref } from 'vue'
import { afterEach, describe, expect, it, vi } from 'vitest'

import type { EmployeeListItemResponse } from '../types/employeeApiTypes'
import { useEmployeeTableConfig } from './useEmployeeTableConfig'

const employee = (overrides: Partial<EmployeeListItemResponse> = {}): EmployeeListItemResponse => ({
  id: 1,
  employeeCode: 'E001',
  employeeName: '一覧確認 太郎',
  employeeNameKana: 'イチランカクニン タロウ',
  gender: 'MALE',
  birthDate: '1990-11-13',
  hireDate: '2026-04-01',
  resignDate: null,
  employmentType: 'FULL_TIME',
  employmentStatus: 'ACTIVE',
  phone: null,
  email: null,
  address: '東京都千代田区テスト1-2-3',
  contractStartDate: '2026-04-01',
  contractEndDate: null,
  activeFlag: true,
  ...overrides,
})

describe('useEmployeeTableConfig', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('住所・生年月日・当日年齢を一覧へ表示する', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 10, 13, 12))

    const { rows, columns } = useEmployeeTableConfig(ref([employee()]))

    expect(rows.value[0]).toMatchObject({
      address: '東京都千代田区テスト1-2-3',
      birthDate: '1990-11-13',
      currentAge: '36',
    })
    expect(columns.value.map((column) => column.title)).toEqual(
      expect.arrayContaining(['住所', '生年月日', '年齢']),
    )
  })

  it('生年月日未登録時は年齢を空欄にする', () => {
    const { rows } = useEmployeeTableConfig(ref([employee({ birthDate: null })]))

    expect(rows.value[0]?.birthDate).toBe('')
    expect(rows.value[0]?.currentAge).toBe('')
  })
})
