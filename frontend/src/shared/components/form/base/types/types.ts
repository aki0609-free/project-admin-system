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

export interface FormSelectOption {
  title: string
  value: any
  props?: {
    disabled?: boolean
  }
}

export interface FormFieldDef<T> {
  key: keyof T
  label: string
  type: FormFieldType
  options?: FormSelectOption[]
  rows?: number
  autoGrow?: boolean
  required?: boolean
  editable?: boolean
  visible?: (model: T) => boolean
  formatter?: (value: any, row: T) => string
  parser?: (value: any, row: T) => any
  /** 利用者が入力欄を操作して値を変更した直後にだけ呼び出す。 */
  onUpdate?: (value: any, row: T) => void
}

export interface FormContext {
  model: ComputedRef<Record<string, any>>
  errors: Ref<Partial<Record<string, string[]>>>
  validateField: (key: string) => boolean
  validateAll: () => boolean
}

export const FormContextKey = Symbol('FormContext') as InjectionKey<FormContext>
