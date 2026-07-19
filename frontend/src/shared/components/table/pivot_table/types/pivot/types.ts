export type PivotAggregation = 'sum' | 'avg' | 'count' | 'min' | 'max'

export interface PivotOptions<T> {
    rowKey: keyof T
    columnKey: keyof T
    valueKey: keyof T

    aggregation?: PivotAggregation

    rowFormatter?: (v: unknown) => string
    columnFormatter?: (v: unknown) => string
    valueFormatter?: (v: number) => string

    filter?: (item: T) => boolean

    sortRows?: (a: string, b: string) => number
    sortColumns?: (a: string, b: string) => number

    showRowTotals?: boolean
    showColumnTotals?: boolean
}