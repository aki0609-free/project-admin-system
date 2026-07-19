export type SimpleTableFilterLogic = 'AND' | 'OR'

export type SimpleTableFilterType =
  | 'text'
  | 'select'
  | 'number'
  | 'checkbox'
  | 'date'

export type SimpleTableFilterPredicate<T> = (
    item: T,
    filterValue: unknown
) => boolean

export interface SimpleTableFilterRule<T> {
    key: string
    predicate: SimpleTableFilterPredicate<T>
}

export type SelectOption = {
    value: unknown
    title: string
}