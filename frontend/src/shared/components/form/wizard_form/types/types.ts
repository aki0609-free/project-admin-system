import { FormFieldDef } from "../../base/types/types"

export interface WizardStep<T> {
    title: string
    fields: FormFieldDef<T>[]
}