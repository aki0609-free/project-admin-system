import type {
  BatchParameterDefinition,
  ToolbarItem,
} from '@/shared/components/toolbar/types/types'

export const createButtonItem = (config: {
  label: string
  onClick: () => void
  color?: string
  disabled?: boolean
}): ToolbarItem => ({
  type: 'button',
  label: config.label,
  color: config.color,
  disabled: config.disabled,
  onClick: config.onClick,
})

export const createBatchItem = (config: {
  label: string
  jobCode: string
  color?: string
  disabled?: boolean
  confirmMessage?: string
  parameterDefinitions?: BatchParameterDefinition[]
  defaultParams?: Record<string, unknown>
  onSuccess?: (message: string) => void
}): ToolbarItem => ({
  type: 'batch',
  label: config.label,
  jobCode: config.jobCode,
  color: config.color ?? 'secondary',
  disabled: config.disabled,
  confirmMessage: config.confirmMessage,
  parameterDefinitions: config.parameterDefinitions ?? [],
  defaultParams: config.defaultParams ?? {},
  onSuccess: config.onSuccess,
})

export const batchParams = {
  text: (config: {
    key: string
    label: string
    required?: boolean
    defaultValue?: string
    gridColumn?: string
  }): BatchParameterDefinition => ({
    key: config.key,
    label: config.label,
    type: 'text',
    required: config.required,
    defaultValue: config.defaultValue ?? '',
    gridColumn: config.gridColumn,
  }),

  number: (config: {
    key: string
    label: string
    required?: boolean
    defaultValue?: number
    gridColumn?: string
  }): BatchParameterDefinition => ({
    key: config.key,
    label: config.label,
    type: 'number',
    required: config.required,
    defaultValue: config.defaultValue ?? '',
    gridColumn: config.gridColumn,
  }),

  date: (config: {
    key: string
    label: string
    required?: boolean
    defaultValue?: string
    gridColumn?: string
  }): BatchParameterDefinition => ({
    key: config.key,
    label: config.label,
    type: 'date',
    required: config.required,
    defaultValue: config.defaultValue ?? '',
    gridColumn: config.gridColumn,
  }),

  month: (config: {
    key: string
    label: string
    required?: boolean
    defaultValue?: string
    gridColumn?: string
  }): BatchParameterDefinition => ({
    key: config.key,
    label: config.label,
    type: 'month',
    required: config.required,
    defaultValue: config.defaultValue ?? '',
    gridColumn: config.gridColumn,
  }),

  select: (config: {
    key: string
    label: string
    required?: boolean
    defaultValue?: string | number | boolean
    options: { title: string; value: string | number | boolean }[]
    gridColumn?: string
  }): BatchParameterDefinition => ({
    key: config.key,
    label: config.label,
    type: 'select',
    required: config.required,
    defaultValue: config.defaultValue ?? '',
    options: config.options,
    gridColumn: config.gridColumn,
  }),

  checkbox: (config: {
    key: string
    label: string
    defaultValue?: boolean
    gridColumn?: string
  }): BatchParameterDefinition => ({
    key: config.key,
    label: config.label,
    type: 'checkbox',
    defaultValue: config.defaultValue ?? false,
    gridColumn: config.gridColumn,
  }),
}