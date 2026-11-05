export type GenericMasterFieldType =
  | 'STRING'
  | 'TEXT'
  | 'NUMBER'
  | 'DATE'
  | 'BOOLEAN'
  | 'SELECT'

export type GenericMasterFieldDefinition = {
  id: number | null
  fieldKey: string
  fieldLabel: string
  fieldType: GenericMasterFieldType
  requiredFlag: boolean
  listVisibleFlag: boolean
  searchableFlag: boolean
  displayOrder: number
  unit: string | null
  placeholder: string | null
  validationPattern: string | null
  optionsJson: string | null
}

export type GenericMasterDefinition = {
  id: number
  masterCode: string
  masterName: string
  tabName: string
  placement: 'BUSINESS_SETTINGS'
  description: string | null
  displayOrder: number
  status: 'DRAFT' | 'PUBLISHED'
  activeFlag: boolean
  fields: GenericMasterFieldDefinition[]
}

export type GenericMasterDefinitionSaveRequest = Omit<
  GenericMasterDefinition,
  'id' | 'status'
>

export type GenericMasterRecord = {
  id: number
  definitionId: number
  values: Record<string, unknown>
  activeFlag: boolean
  lockVersion: number
  updatedAt: string
}

export type GenericMasterRecordSaveRequest = Pick<
  GenericMasterRecord,
  'values' | 'activeFlag'
> & { lockVersion: number | null }

export type GenericMasterSelectOption = {
  title: string
  value: string | number | boolean
}
