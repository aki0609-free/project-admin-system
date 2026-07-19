import type { RuleMasterResponse, RuleMasterSaveRequest } from "../types/ruleApiTypes"
import type { RuleParameterForm, RuleColumnMappingForm, RuleDataSourceForm, RuleMasterForm } from "../types/ruleFormTypes"


let tempParameterId = -1
let tempDataSourceId = -1000
let tempColumnId = -2000

export const createEmptyRuleParameter = (
  orderNo = 1,
): RuleParameterForm => ({
  id: tempParameterId--,
  paramName: '',
  dataType: 'STRING',
  requiredFlag: false,
  defaultValue: '',
  description: '',
  orderNo,
})

export const createEmptyRuleColumn = (
  orderNo = 1,
): RuleColumnMappingForm => ({
  id: tempColumnId--,
  columnName: '',
  factKey: '',
  dataType: 'STRING',
  requiredFlag: false,
  orderNo,
})

export const createEmptyRuleDataSource = (
  orderNo = 1,
): RuleDataSourceForm => ({
  id: tempDataSourceId--,
  sourceName: '',
  tableName: '',
  whereClause: '',
  singleRowFlag: true,
  activeFlag: true,
  orderNo,
  columns: [],
})

export const createEmptyRuleMasterForm = (): RuleMasterForm => ({
  id: 0,
  ruleName: '',
  ruleDisplayName: '',
  ruleType: 'GENERAL',
  dslType: 'JEXL',
  dslText: '',
  ruleBeanName: '',
  resultFactKey: 'result',
  description: '',
  priority: 100,
  activeFlag: true,
  parameters: [],
  dataSources: [],
})

export const toRuleMasterForm = (
  rule: RuleMasterResponse,
): RuleMasterForm => ({
  id: rule.id,
  ruleName: rule.ruleName,
  ruleDisplayName: rule.ruleDisplayName,
  ruleType: rule.ruleType,
  dslType: rule.dslType,
  dslText: rule.dslText ?? '',
  ruleBeanName: rule.ruleBeanName ?? '',
  resultFactKey: rule.resultFactKey ?? 'result',
  description: rule.description ?? '',
  priority: rule.priority,
  activeFlag: rule.activeFlag,
  parameters: rule.parameters.map(parameter => ({
    id: parameter.id,
    paramName: parameter.paramName,
    dataType: parameter.dataType,
    requiredFlag: parameter.requiredFlag,
    defaultValue: parameter.defaultValue ?? '',
    description: parameter.description ?? '',
    orderNo: parameter.orderNo,
  })),
  dataSources: rule.dataSources.map(source => ({
    id: source.id,
    sourceName: source.sourceName,
    tableName: source.tableName,
    whereClause: source.whereClause ?? '',
    singleRowFlag: source.singleRowFlag,
    activeFlag: source.activeFlag,
    orderNo: source.orderNo,
    columns: source.columns.map(column => ({
      id: column.id,
      columnName: column.columnName,
      factKey: column.factKey,
      dataType: column.dataType,
      requiredFlag: column.requiredFlag,
      orderNo: column.orderNo,
    })),
  })),
})

export const toRuleMasterSaveRequest = (
  form: RuleMasterForm,
): RuleMasterSaveRequest => ({
  ruleName: form.ruleName,
  ruleDisplayName: form.ruleDisplayName,
  ruleType: form.ruleType,
  dslType: form.dslType,
  dslText: form.dslText || null,
  ruleBeanName: form.ruleBeanName || null,
  resultFactKey: form.resultFactKey || 'result',
  description: form.description || null,
  priority: form.priority,
  activeFlag: form.activeFlag,
  parameters: form.parameters.map(parameter => ({
    id: parameter.id > 0 ? parameter.id : null,
    paramName: parameter.paramName,
    dataType: parameter.dataType,
    requiredFlag: parameter.requiredFlag,
    defaultValue: parameter.defaultValue || null,
    description: parameter.description || null,
    orderNo: parameter.orderNo,
  })),
  dataSources: form.dataSources.map(source => ({
    id: source.id > 0 ? source.id : null,
    sourceName: source.sourceName,
    tableName: source.tableName,
    whereClause: source.whereClause || null,
    singleRowFlag: source.singleRowFlag,
    activeFlag: source.activeFlag,
    orderNo: source.orderNo,
    columns: source.columns.map(column => ({
      id: column.id > 0 ? column.id : null,
      columnName: column.columnName,
      factKey: column.factKey,
      dataType: column.dataType,
      requiredFlag: column.requiredFlag,
      orderNo: column.orderNo,
    })),
  })),
})