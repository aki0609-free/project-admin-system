import { describe, expect, it } from 'vitest'
import { createEmptyEmployeeForm } from '../utils/employeeFormFactory'
import { validateEmployeeForm } from './employeeSchemas'

describe('validateEmployeeForm', () => {
  const validForm = () => ({
    ...createEmptyEmployeeForm(),
    employeeCode: 'E001',
    employeeName: 'テスト従業員',
  })

  it('accepts the default payroll and contract settings with required basic values', () => {
    expect(validateEmployeeForm(validForm())).toBeNull()
  })

  it('rejects a whitespace-only employee name on the basic tab', () => {
    const form = validForm()
    form.employeeName = '   '

    expect(validateEmployeeForm(form)).toEqual({
      tab: 'basic',
      message: '氏名は必須です。',
    })
  })

  it('rejects a required employee payroll-item parameter when it is missing', () => {
    const form = validForm()
    form.payrollItemSettings = [
      {
        targetType: 'DEDUCTION',
        targetCode: 'DORMITORY_FEE',
        displayName: '寮費',
        enabled: true,
        effectiveFrom: '',
        effectiveTo: '',
        inputSource: 'DAILY_REPORT',
        balanceTracked: true,
        balanceUnit: 'DAYS',
        openingQuantity: 0,
        accruedQuantity: 0,
        consumedQuantity: 0,
        remainingQuantity: 0,
        parameters: {},
        parameterDefinitions: [
          {
            key: 'roomType',
            displayName: '寮タイプ',
            inputType: 'SELECT',
            required: true,
            defaultValue: null,
            options: [{ label: '一人部屋', value: 'SINGLE_ROOM' }],
            ruleParameter: true,
            dailyDisplay: false,
            inputSourceOverride: false,
            ruleValueResolverKey: null,
            displayOrder: 1,
          },
        ],
      },
    ]

    expect(validateEmployeeForm(form)).toEqual({
      tab: 'payrollItems',
      message: '寮費の「寮タイプ」は必須です。',
    })
  })

  it('rejects a negative payroll value on the payroll tab', () => {
    const form = validForm()
    form.payrollProfile.residentTaxMonthly = -1

    expect(validateEmployeeForm(form)?.tab).toBe('payroll')
  })

  it('rejects a contract end date before its start date', () => {
    const form = validForm()
    form.contract.contractStartDate = '2026-08-10'
    form.contract.contractEndDate = '2026-08-09'

    expect(validateEmployeeForm(form)).toEqual({
      tab: 'contract',
      message: '契約終了日は契約開始日以降で指定してください。',
    })
  })
})
