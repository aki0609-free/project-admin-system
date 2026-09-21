import { describe, expect, it } from 'vitest'

import { useCustomerFormFields } from './useCustomerFormFields'

describe('useCustomerFormFields', () => {
  it('顧客距離請求単価を円/kmで表示する', () => {
    const { fields } = useCustomerFormFields()
    const distanceField = fields.value.find(
      field => field.key === 'distanceBillingUnitPrice',
    )

    expect(distanceField?.label).toBe('顧客距離請求単価')
    expect(distanceField?.formatter?.(30, {} as never)).toBe('30円/km')
  })
})
