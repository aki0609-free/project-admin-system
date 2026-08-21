/* eslint-disable @typescript-eslint/no-explicit-any */
import type { ComputedRef, InjectionKey, Ref } from 'vue'

export type FormFieldType =
  | 'text'
  | 'password'
  | 'number'
  | 'select'
  | 'checkbox'
  | 'date'
  | 'month'
  | 'time'
  | 'object'
  | 'array'
  | 'selectboxWithChips'
  | 'dayrule'
  | 'textarea'
  | 'sqlEditor'

export interface FormFieldDef<T> {
  key: keyof T
  label: string
  type: FormFieldType
  options?: { title: string; value: any }[]
  rows?: number
  autoGrow?: boolean
  required?: boolean
  editable?: boolean
  visible?: (model: T) => boolean
  formatter?: (value: any, row: T) => string
}

export interface FormContext {
  model: ComputedRef<Record<string, any>>
  errors: Ref<Partial<Record<string, string[]>>>
  validateField: (key: string) => boolean
  validateAll: () => boolean
}

export const FormContextKey = Symbol('FormContext') as InjectionKey<FormContext>
