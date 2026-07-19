import { type FormFieldDef } from '@/shared/components/form/base/types/types'

export interface GridFormFieldDef<T> extends FormFieldDef<T> {
  /** CSS Grid の grid-column 指定用 */
  gridColumn?: string
  width?: number
}