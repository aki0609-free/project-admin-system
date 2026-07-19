export type PivotFilterLogic = 'AND' | 'OR'

export type PivotFilterType = 'text' | 'select' | 'number' | 'checkbox'

export type PivotFilterPredicate<T> = (
    item: T,
    filterValue: unknown
) => boolean

export interface PivotFilterRule<T> {
    key: keyof T
    predicate: PivotFilterPredicate<T>
    axis?: 'row' | 'column' | 'value'
}
