export type BatchParameterType = 'text' | 'number' | 'date' | 'month' | 'select' | 'checkbox'

export type BatchParameterOption = {
  title: string
  value: string | number | boolean
}

export type BatchParameterDefinition = {
  key: string
  label: string
  type: BatchParameterType
  required?: boolean
  defaultValue?: unknown
  options?: BatchParameterOption[]
  gridColumn?: string
  width?: number
}

export type ToolbarIntent = 'primary' | 'secondary' | 'danger' | 'warning' | 'utility'

type ToolbarItemBase = {
  id?: string
  visible?: boolean
  disabled?: boolean
  loading?: boolean
  tooltip?: string
  intent?: ToolbarIntent
}

export type ToolbarButtonItem = ToolbarItemBase & {
  type: 'button'
  label: string
  onClick: () => void
  color?: string
}

export type ToolbarBatchItem = ToolbarItemBase & {
  type: 'batch'
  label: string
  jobCode: string
  color?: string
  confirmMessage?: string
  parameterDefinitions?: BatchParameterDefinition[]
  defaultParams?: Record<string, unknown>
  outputAction?: 'none' | 'download' | 'preview'
  onSuccess?: (message: string) => void
}

export type ToolbarIconItem = ToolbarItemBase & {
  type: 'icon'
  icon: string
  label?: string
  color?: string
  onClick: () => void
}

export type ToolbarDropdownItem = ToolbarItemBase & {
  type: 'dropdown'
  label: string
  color?: string
  options: { label: string; value: string }[]
  onSelect: (value: string) => void
}

export type ToolbarSearchItem = ToolbarItemBase & {
  type: 'search'
  modelValue: string
  'onUpdate:modelValue': (value: string) => void
  placeholder?: string
}

export type ToolbarItem =
  | ToolbarButtonItem
  | ToolbarBatchItem
  | ToolbarIconItem
  | ToolbarDropdownItem
  | ToolbarSearchItem
