export type SystemSettingValueType = 'STRING' | 'NUMBER' | 'BOOLEAN' | 'URL' | 'DATE' | 'JSON'

export type SystemSettingItem = {
  definitionId: number
  largeCategoryKey: string
  largeCategoryName: string
  middleCategoryKey: string
  middleCategoryName: string
  smallCategoryKey: string
  settingName: string
  description: string | null
  valueType: SystemSettingValueType
  requiredFlag: boolean
  placeholder: string | null
  displayOrder: number
  value: string
}

export type SystemSettingValueSaveRequest = {
  values: Array<{
    definitionId: number
    value: string
  }>
}

export type SystemSettingScope = 'SYSTEM' | 'TENANT'

export type SystemSettingDefinition = {
  id: number
  scope: SystemSettingScope
  largeCategoryKey: string
  largeCategoryName: string
  middleCategoryKey: string
  middleCategoryName: string
  smallCategoryKey: string
  settingName: string
  description: string | null
  valueType: SystemSettingValueType
  requiredFlag: boolean
  validationPattern: string | null
  placeholder: string | null
  displayOrder: number
  sensitiveFlag: boolean
  activeFlag: boolean
  systemManagedFlag: boolean
  editable: boolean
  settingValue: string
}

export type SystemSettingDefinitionCreateRequest = Omit<
  SystemSettingDefinition,
  'id' | 'scope' | 'systemManagedFlag' | 'editable' | 'settingValue'
> & { settingValue: string }

export type SystemSettingDefinitionUpdateRequest = Omit<
  SystemSettingDefinitionCreateRequest,
  'largeCategoryKey' | 'middleCategoryKey' | 'smallCategoryKey'
>
