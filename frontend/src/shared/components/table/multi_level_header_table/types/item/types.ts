import type {
  MultiLevelHeaderTableFilterPredicate,
  MultiLevelHeaderTableFilterRule,
  MultiLevelHeaderTableFilterType,
} from '../filter/types'

export type MultiLevelHeaderAggregation = 'sum' | 'avg' | 'count'
export type MultiLevelHeaderAxis<T> = {
  key: keyof T
  title: string
  filterType?: MultiLevelHeaderTableFilterType
  formatter?: (value: unknown, row: T) => string

  filter?: {
    type: MultiLevelHeaderTableFilterType
    predicate?: MultiLevelHeaderTableFilterPredicate<T>
  }
  width?: string
  align?: 'left' | 'center' | 'right'
}

export type MultiLevelHeaderColumnDef<T> =
  | MultiLevelHeaderGroupColumn<T>
  | MultiLevelHeaderLeafColumn<T>

export type MultiLevelHeaderGroupColumn<T> = {
  key: string
  title: string
  subColumns: MultiLevelHeaderColumnDef<T>[]

  width?: string
  align?: 'left' | 'center' | 'right'
}

export type MultiLevelHeaderLeafColumn<T> = {
  key: keyof T
  title: string
  type: MultiLevelHeaderTableFilterType

  formatter?: (value: unknown, row: T) => string
  aggregation?: MultiLevelHeaderAggregation

  editable?: boolean
  required?: boolean

  enumOptions?: {
    value: string
    title: string
  }[]

  width?: string
  align?: 'left' | 'center' | 'right'

  filter?: {
    type: MultiLevelHeaderTableFilterType
    predicate?: MultiLevelHeaderTableFilterPredicate<T>
  }

  subColumns?: undefined
}

export type MultiLevelHeaderTableDef<T> = {
  row: MultiLevelHeaderAxis<T>
  columns: MultiLevelHeaderColumnDef<T>[]

  filter?: MultiLevelHeaderTableFilterRule<T>[]
}

export type MultiLevelHeaderTableEditableRow = {
  id: number
  _isNew?: boolean
  _isUpdated?: boolean
  _isDeleted?: boolean
}
