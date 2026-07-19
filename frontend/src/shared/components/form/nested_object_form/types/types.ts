import { boolean } from "zod";
import { FormFieldDef } from "../../base/types/types";

export interface NestedObjectFormFieldDef<T> extends FormFieldDef<T> {
    fields?: NestedObjectFormFieldDef<any>[]
    itemFields?: NestedObjectFormFieldDef<any>[]
    showIf?: (model: T) => boolean
}