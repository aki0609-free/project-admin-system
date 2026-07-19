export type RuleType =
  | 'ALLOWANCE'
  | 'DEDUCTION'
  | 'DAILY_REPORT'
  | 'MONTHLY_DETAIL'
  | 'PAYROLL'
  | 'GENERAL'

export type RuleDslType =
  | 'JEXL'
  | 'MVEL'
  | 'JAVA_BEAN'

export type RuleDataType =
  | 'STRING'
  | 'INTEGER'
  | 'LONG'
  | 'DECIMAL'
  | 'BOOLEAN'
  | 'DATE'
  | 'DATETIME'

export type RuleParameterResponse = {
  id: number
  paramName: string
  dataType: RuleDataType
  requiredFlag: boolean
  defaultValue: string | null
  description: string | null
  orderNo: number
}

export type RuleColumnMappingResponse = {
  id: number
  columnName: string
  factKey: string
  dataType: RuleDataType
  requiredFlag: boolean
  orderNo: number
}

export type RuleDataSourceResponse = {
  id: number
  sourceName: string
  tableName: string
  whereClause: string | null
  singleRowFlag: boolean
  activeFlag: boolean
  orderNo: number
  columns: RuleColumnMappingResponse[]
}

export type RuleMasterResponse = {
  id: number
  ruleName: string
  ruleDisplayName: string
  ruleType: RuleType
  dslType: RuleDslType
  dslText: string | null
  ruleBeanName: string | null
  resultFactKey: string
  description: string | null
  priority: number
  activeFlag: boolean
  parameters: RuleParameterResponse[]
  dataSources: RuleDataSourceResponse[]
}

export type RuleParameterSaveRequest = {
  id: number | null
  paramName: string
  dataType: RuleDataType
  requiredFlag: boolean
  defaultValue: string | null
  description: string | null
  orderNo: number
}

export type RuleColumnMappingSaveRequest = {
  id: number | null
  columnName: string
  factKey: string
  dataType: RuleDataType
  requiredFlag: boolean
  orderNo: number
}

export type RuleDataSourceSaveRequest = {
  id: number | null
  sourceName: string
  tableName: string
  whereClause: string | null
  singleRowFlag: boolean
  activeFlag: boolean
  orderNo: number
  columns: RuleColumnMappingSaveRequest[]
}

export type RuleMasterSaveRequest = {
  ruleName: string
  ruleDisplayName: string
  ruleType: RuleType
  dslType: RuleDslType
  dslText: string | null
  ruleBeanName: string | null
  resultFactKey: string
  description: string | null
  priority: number
  activeFlag: boolean
  parameters: RuleParameterSaveRequest[]
  dataSources: RuleDataSourceSaveRequest[]
}

export type RuleExecutionRequest = {
  ruleName: string
  facts: Record<string, unknown>
}

export type RuleExecutionResponse = {
  ruleName: string
  executed: boolean
  result: unknown
  facts: Record<string, unknown>
  message: string
}