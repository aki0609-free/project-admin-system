import { FormFieldDef } from "../../base/types/types";

export interface DynamicFormFieldDef<T> extends FormFieldDef<T> {
    showIf?: (model: T) => boolean
}