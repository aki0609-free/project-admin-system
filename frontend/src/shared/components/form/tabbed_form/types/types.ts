import { FormFieldDef } from '@/shared/components/form/base/types/types'

export interface TabbedFormFieldDef<T> extends FormFieldDef<T> {
  tab?: string       // このフィールドが属するタブ
  gridColumn?: string // グリッド列指定（オプション）
}