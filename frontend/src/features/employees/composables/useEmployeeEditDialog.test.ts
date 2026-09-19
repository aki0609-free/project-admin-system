import { describe, expect, it } from 'vitest'

import { employeeTaxCategoryOptions } from './useEmployeeEditDialog'

describe('employeeTaxCategoryOptions', () => {
  it('V1では甲だけを選択可能にする', () => {
    expect(employeeTaxCategoryOptions).toEqual([
      { title: '甲', value: 'KOU' },
      { title: '乙', value: 'OTSU', props: { disabled: true } },
      { title: '丙', value: 'HEI', props: { disabled: true } },
    ])
  })
})
