import { ref } from 'vue'
import { describe, expect, it } from 'vitest'

import {
  employeeTaxCategoryOptions,
  useEmployeeEditDialog,
} from './useEmployeeEditDialog'

describe('employeeTaxCategoryOptions', () => {
  it('V1では甲だけを選択可能にする', () => {
    expect(employeeTaxCategoryOptions).toEqual([
      { title: '甲', value: 'KOU' },
      { title: '乙', value: 'OTSU', props: { disabled: true } },
      { title: '丙', value: 'HEI', props: { disabled: true } },
    ])
  })

  it('給与・契約の数値項目を適切な単位で表示する', () => {
    const { payrollFields, contractFields } = useEmployeeEditDialog(
      ref(false),
      ref(null),
      () => undefined,
      () => undefined,
      () => undefined,
    )

    expect(
      payrollFields.find(
        field => field.key === 'taxDependentCount',
      )?.formatter?.(2, {} as never),
    ).toBe('2人')
    expect(
      payrollFields.find(
        field => field.key === 'paidLeaveRemainingDays',
      )?.formatter?.(3.5, {} as never),
    ).toBe('3.5日')
    expect(
      contractFields.find(
        field => field.key === 'dailyWage',
      )?.formatter?.(12000, {} as never),
    ).toBe('12,000円')
    expect(
      contractFields.find(
        field => field.key === 'standardWorkingHours',
      )?.formatter?.(8, {} as never),
    ).toBe('8時間')
  })
})
