import { reactive } from 'vue'
import { describe, expect, it } from 'vitest'
import {
  clonePayrollItemPolicy,
  createDefaultPayrollItemPolicy,
} from './payrollItemPolicyTypes'

describe('clonePayrollItemPolicy', () => {
  it('Vue Proxyをプレーンな保存用データへ変換する', () => {
    const policy = reactive(createDefaultPayrollItemPolicy())
    policy.parameterDefinitions.push({
      key: 'dormitoryType',
      displayName: '寮タイプ',
      inputType: 'SELECT',
      required: true,
      defaultValue: 'SHARED',
      options: [
        { label: '相部屋', value: 'SHARED', calculationValue: 500 },
      ],
      ruleParameter: true,
      dailyDisplay: true,
      inputSourceOverride: false,
      ruleValueResolverKey: 'SELECT_OPTION_CALCULATION_VALUE:dormitoryType',
      displayOrder: 10,
    })

    const cloned = clonePayrollItemPolicy(policy)

    expect(() => structuredClone(cloned)).not.toThrow()
    expect(cloned).toEqual(policy)

    const clonedOption = cloned.parameterDefinitions.at(0)?.options.at(0)
    const originalOption = policy.parameterDefinitions.at(0)?.options.at(0)
    expect(clonedOption).toBeDefined()
    expect(originalOption).toBeDefined()
    if (!clonedOption || !originalOption) throw new Error('テスト用の選択肢がありません')

    clonedOption.label = '変更後'
    expect(originalOption.label).toBe('相部屋')
  })
})
