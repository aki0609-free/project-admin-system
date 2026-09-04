export type PayrollItemApplicationScope = 'ALL_EMPLOYEES' | 'EMPLOYEE_ENROLLMENT'
export type PayrollItemInputSource =
  | 'DAILY_REPORT'
  | 'TRANSACTION'
  | 'DAILY_REPORT_AND_TRANSACTION'
export type PayrollItemBalanceUnit = 'AMOUNT' | 'DAYS' | 'HOURS' | 'COUNT'
export type PayrollItemParameterInputType = 'TEXT' | 'NUMBER' | 'SELECT' | 'BOOLEAN' | 'DATE'

export type PayrollItemParameterOption = {
  label: string
  value: string
  calculationValue?: number | null
}

export type PayrollItemParameterDefinition = {
  key: string
  displayName: string
  inputType: PayrollItemParameterInputType
  required: boolean
  defaultValue: string | null
  options: PayrollItemParameterOption[]
  ruleParameter: boolean
  dailyDisplay: boolean
  inputSourceOverride: boolean
  ruleValueResolverKey?: string | null
  displayOrder: number
}

export type PayrollItemPolicy = {
  applicationScope: PayrollItemApplicationScope
  inputSource: PayrollItemInputSource
  balanceTracking: boolean
  balanceUnit: PayrollItemBalanceUnit
  accrualFrequency: string
  accrualRuleName: string
  carryForward: boolean
  advanceConsumption: boolean
  parameterDefinitions: PayrollItemParameterDefinition[]
}

export const createDefaultPayrollItemPolicy = (): PayrollItemPolicy => ({
  applicationScope: 'ALL_EMPLOYEES',
  inputSource: 'DAILY_REPORT',
  balanceTracking: false,
  balanceUnit: 'AMOUNT',
  accrualFrequency: 'MANUAL',
  accrualRuleName: 'MANUAL_TRANSACTION',
  carryForward: false,
  advanceConsumption: false,
  parameterDefinitions: [],
})

/**
 * Vueのreactive/readonly Proxyを保存リクエストへ持ち込まないための明示的なコピー。
 * structuredCloneはProxyを複製できないため、帳票項目ポリシーの境界ではこの関数を使う。
 */
export const clonePayrollItemPolicy = (policy: PayrollItemPolicy): PayrollItemPolicy => ({
  applicationScope: policy.applicationScope,
  inputSource: policy.inputSource,
  balanceTracking: policy.balanceTracking,
  balanceUnit: policy.balanceUnit,
  accrualFrequency: policy.accrualFrequency,
  accrualRuleName: policy.accrualRuleName,
  carryForward: policy.carryForward,
  advanceConsumption: policy.advanceConsumption,
  parameterDefinitions: policy.parameterDefinitions.map(definition => ({
    key: definition.key,
    displayName: definition.displayName,
    inputType: definition.inputType,
    required: definition.required,
    defaultValue: definition.defaultValue,
    options: definition.options.map(option => ({
      label: option.label,
      value: option.value,
      calculationValue: option.calculationValue ?? null,
    })),
    ruleParameter: definition.ruleParameter,
    dailyDisplay: definition.dailyDisplay,
    inputSourceOverride: definition.inputSourceOverride,
    ruleValueResolverKey: definition.ruleValueResolverKey ?? null,
    displayOrder: definition.displayOrder,
  })),
})
