import { PivotFilterPredicate, PivotFilterRule, PivotFilterType } from "../filter/types"
import { PivotAggregation } from "../pivot/types"

// Pivot
export type PivotAxis<T> = {
    key: keyof T
    title: string

    filterType?: PivotFilterType
    formatter?: (value: unknown, row: T) => string
}

export type PivotValueDef<T> = {
    key: keyof T

    filterType?: PivotFilterType
    formatter?: (value: unknown, row: T) => string

    aggregation?: PivotAggregation

    editable?: boolean
    required?: boolean
}

export type PivotFilter<T> = {
    type: PivotFilterType
    predicate?: PivotFilterPredicate<T>
}

export type PivotTableDef<T> = {
    row: PivotAxis<T>
    column: PivotAxis<T>

    values: PivotValueDef<T>[]

    filter?: PivotFilterRule<T>[]
}

