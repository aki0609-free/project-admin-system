export type MultiLevelHeaderTableFilterLogic = 'AND' | 'OR'
export type MultiLevelHeaderTableFilterType = 'text' | 'select' | 'number' | 'checkbox' | 'date'

export type MultiLevelHeaderTableFilterPredicate<T> = (
    item: T,
    filterValue: unknown
) => boolean

export interface MultiLevelHeaderTableFilterRule<T> {
  key: keyof T
  type: MultiLevelHeaderTableFilterType
  predicate: (item: T, value: unknown) => boolean
}
