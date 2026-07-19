/* eslint-disable @typescript-eslint/no-explicit-any */
import type { ComputedRef, InjectionKey, Ref } from "vue"
import type { z } from 'zod'

export type FormFieldType =
  | 'text'
  | 'password'
  | 'number'
  | 'select'
  | 'checkbox'
  | 'date'
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
  formatter?: (value: any, row: T) => string
}

export interface FormContext {
  model: ComputedRef<Record<string, any>>
  errors: Ref<Partial<Record<string, string[]>>>
  validateField: (key: string) => boolean
  validateAll: () => boolean
}

export interface NestedFormContext<T = any> extends FormContext {
  zodError: Ref<z.ZodError<T> | null>
}

export const FormContextKey = Symbol('FormContext') as InjectionKey<FormContext>
export const NestedFormContextKey = Symbol('NestedFormContextKey') as InjectionKey<NestedFormContext>