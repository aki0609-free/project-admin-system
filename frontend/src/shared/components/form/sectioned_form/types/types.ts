import { FormFieldDef } from "../../base/types/types"

export interface FormSection<T> {
  title: string
  fields: FormFieldDef<T>[]
}