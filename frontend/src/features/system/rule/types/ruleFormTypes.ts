import type {
  RuleDataType,
  RuleDslType,
  RuleType,
} from './ruleApiTypes'

export type RuleParameterForm = {
  id: number
  paramName: string
  dataType: RuleDataType
  requiredFlag: boolean
  defaultValue: string
  description: string
  orderNo: number
}

export type RuleColumnMappingForm = {
  id: number
  columnName: string
  factKey: string
  dataType: RuleDataType
  requiredFlag: boolean
  orderNo: number
}

export type RuleDataSourceForm = {
  id: number
  sourceName: string
  tableName: string
  whereClause: string
  singleRowFlag: boolean
  activeFlag: boolean
  orderNo: number
  columns: RuleColumnMappingForm[]
}

export type RuleMasterForm = {
  id: number
  ruleName: string
  ruleDisplayName: string
  ruleType: RuleType
  dslType: RuleDslType
  dslText: string
  ruleBeanName: string
  resultFactKey: string
  description: string
  priority: number
  activeFlag: boolean
  parameters: RuleParameterForm[]
  dataSources: RuleDataSourceForm[]
}