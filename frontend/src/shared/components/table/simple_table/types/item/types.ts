import { type SelectOption, type SimpleTableFilterPredicate } from "../filter/types"

type SimpleTableColumnType = 'text' | 'date' | 'select' | 'checkbox' | 'number' | 'dayrule'
type SimpleTableFilterType = 'text' | 'select' | 'number' | 'checkbox' | 'date'

export type SimpleTableColumnFilter<T> = {
    type: SimpleTableFilterType
    multiple?: boolean
    predicate?: SimpleTableFilterPredicate<T>
}

export type SimpleTableColumnDef<T> = {
    title: string
    key: keyof T
    type?: SimpleTableColumnType
    filter?: SimpleTableColumnFilter<T>
    editable?: boolean
    required?: boolean

    width?: string

    enumOptions?: SelectOption[]

    formatter?: (value: unknown, row: T) => string

    valueGetter?: (row: T) => unknown
    computed?: boolean
}

export type SimpleTableEditableRow = {
    id: number
    _isNew?: boolean
    _isUpdated?: boolean
    _isDeleted?: boolean
}