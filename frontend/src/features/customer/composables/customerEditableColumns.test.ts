import { describe, expect, it } from 'vitest'
import { ref } from 'vue'

import { useCustomerEmployeeColumns } from './useCustomerEmployeeColumns'
import { useCustomerSiteColumns } from './useCustomerSiteColumns'
import { useCustomerBillingRateColumns } from './useCustomerSiteBillingRateColumns'

import type { CustomerSite } from '../types/customerTypes'

const sites: CustomerSite[] = [
  {
    id: 5,
    customerId: 22,
    name: '第一現場',
    contactPersonName: '',
    contactPersonPhone: '',
    contactPersonEmail: '',
    distanceFromCompanyKm: null,
  },
  {
    id: 6,
    customerId: 22,
    name: '第二現場',
    contactPersonName: '',
    contactPersonPhone: '',
    contactPersonEmail: '',
    distanceFromCompanyKm: null,
  },
]

const firstSite = sites[0]
if (!firstSite) {
  throw new Error('顧客現場テストデータがありません。')
}

describe('customer editable table columns', () => {
  it('新規現場・顧客社員の一時IDを表示しない', () => {
    const siteIdColumn = useCustomerSiteColumns().columns.value
      .find(column => column.key === 'id')
    const employeeIdColumn = useCustomerEmployeeColumns().columns.value
      .find(column => column.key === 'id')

    expect(siteIdColumn?.formatter?.(-10, firstSite)).toBe('')
    expect(siteIdColumn?.formatter?.(5, firstSite)).toBe('5')
    expect(employeeIdColumn?.formatter?.(-20, {} as never)).toBe('')
  })

  it('距離は0以上の数値入力としてkm表示する', () => {
    const distanceColumn = useCustomerSiteColumns().columns.value
      .find(column => column.key === 'distanceFromCompanyKm')

    expect(distanceColumn).toMatchObject({
      type: 'number',
      editable: true,
      min: 0,
      suffix: 'km',
    })
    expect(distanceColumn?.formatter?.(12, firstSite)).toBe('12km')
  })

  it('保存済みの全現場を名称付き請求単価候補にする', () => {
    const { columns } = useCustomerBillingRateColumns(ref(sites))
    const siteColumn = columns.value
      .find(column => column.key === 'customerSiteId')

    expect(siteColumn?.enumOptions).toEqual([
      { title: '第一現場', value: 5 },
      { title: '第二現場', value: 6 },
    ])
    expect(siteColumn?.formatter?.(6, {} as never)).toBe('第二現場')
  })

  it('全ての請求単価を0以上の数値入力にする', () => {
    const { columns } = useCustomerBillingRateColumns(ref(sites))
    const priceKeys = [
      'baseUnitPrice',
      'overtimeUnitPrice',
      'nightUnitPrice',
      'holidayUnitPrice',
      'commuteUnitPrice',
    ]

    for (const key of priceKeys) {
      expect(columns.value.find(column => column.key === key)).toMatchObject({
        type: 'number',
        editable: true,
        min: 0,
        suffix: '円',
      })
    }
  })
})
