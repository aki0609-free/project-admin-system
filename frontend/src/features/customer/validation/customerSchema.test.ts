import { describe, expect, it } from 'vitest'

import { customerSchema } from './customerSchema'

const validCustomer = {
  name: '株式会社サンプル',
  furiganaName: '',
  shortName: '',
  postNo: '',
  address: '',
  representativeName: '',
  phone: '',
  jobType: '',
  contractFlag: '',
  invoiceType: 'PATTERN_1' as const,
  closingDayRule: null,
  paymentDayRule: null,
}

describe('customerSchema', () => {
  it('有効な顧客情報を受け付ける', () => {
    expect(customerSchema.safeParse(validCustomer).success).toBe(true)
  })

  it('顧客名の空文字と範囲外の締日を拒否する', () => {
    expect(customerSchema.safeParse({ ...validCustomer, name: '  ' }).success).toBe(false)

    const result = customerSchema.safeParse({
      ...validCustomer,
      closingDayRule: {
        type: 'DAY_OF_MONTH',
        value: 32,
        monthOffset: 0,
      },
    })
    expect(result.success).toBe(false)
  })

  it('月末指定で日付値がなくても受け付ける', () => {
    const result = customerSchema.safeParse({
      ...validCustomer,
      closingDayRule: {
        type: 'END_OF_MONTH',
        value: null,
        monthOffset: 0,
      },
    })
    expect(result.success).toBe(true)
  })
})
