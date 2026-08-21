export type PayrollItemApplicationScope = 'ALL_EMPLOYEES' | 'EMPLOYEE_ENROLLMENT'
export type PayrollItemInputSource = 'DAILY_REPORT' | 'TRANSACTION'
export type PayrollItemBalanceUnit = 'AMOUNT' | 'DAYS' | 'HOURS' | 'COUNT'
export type PayrollItemParameterInputType = 'TEXT' | 'NUMBER' | 'SELECT' | 'BOOLEAN' | 'DATE'

export type PayrollItemParameterOption = {
  label: string
  value: string
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
