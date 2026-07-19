import { PivotFilterRule } from "../types/filter/types";
import { PivotTableDef } from "../types/item/types";
import { defaultPivotPredicates } from "./defaultPivotPredicates";

export const createPivotFilterRules = <T extends Record<string, any>>(
    def: PivotTableDef<T>
): PivotFilterRule<T>[] => {
    const rules: PivotFilterRule<T>[] = []

    // row
    const rowType = def.row.filterType ?? 'text'
    rules.push({
        key: def.row.key,
        axis: 'row',
        predicate: defaultPivotPredicates[rowType](def.row.key as string),
    })

    // column
    const colType = def.column.filterType ?? 'text'
    rules.push({
        key: def.column.key,
        axis: 'column',
        predicate: defaultPivotPredicates[colType](def.column.key as string),
    })

    def.values.forEach((v) => {
        const valueType = v.filterType ?? 'number'

        rules.push({
            key: v.key,
            axis: 'value',
            predicate: defaultPivotPredicates[valueType](v.key as string)
        })
    })

    return rules;
}