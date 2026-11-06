import { describe, expect, it } from 'vitest'
import { createPayrollItemRuleGatewayKey } from './ruleGatewayKeys'

describe('createPayrollItemRuleGatewayKey', () => {
  it('creates a stable dynamic key from target type and master id', () => {
    expect(createPayrollItemRuleGatewayKey('ALLOWANCE', 42)).toBe(
      'PAYROLL_ITEM.ALLOWANCE.42',
    )
  })

  it('rejects a non-positive master id', () => {
    expect(() => createPayrollItemRuleGatewayKey('DEDUCTION', 0)).toThrow(
      'masterIdには正の整数を指定してください',
    )
  })
})
