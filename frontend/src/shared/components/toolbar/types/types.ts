export type BatchParameterType =
  | 'text'
  | 'number'
  | 'date'
  | 'month'
  | 'select'
  | 'checkbox'

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

export type ToolbarItem =
  | {
      type: 'button'
      label: string
      onClick: () => void
      color?: string
      disabled?: boolean
    }
  | {
      type: 'batch'
      label: string
      jobCode: string
      color?: string
      disabled?: boolean
      confirmMessage?: string
      parameterDefinitions?: BatchParameterDefinition[]
      defaultParams?: Record<string, unknown>
      onSuccess?: (message: string) => void
    }
  | {
      type: 'icon'
      icon: string
      onClick: () => void
      disabled?: boolean
    }
  | {
      type: 'dropdown'
      label: string
      options: { label: string; value: string }[]
      onSelect: (value: string) => void
      disabled?: boolean
    }
  | {
      type: 'search'
      modelValue: string
      'onUpdate:modelValue': (value: string) => void
      placeholder?: string
      disabled?: boolean
    }