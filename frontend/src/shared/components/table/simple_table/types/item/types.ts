import { type SelectOption, type SimpleTableFilterPredicate } from "../filter/types"

type SimpleTableColumnType = 'text' | 'date' | 'select' | 'checkbox' | 'number' | 'dayrule'
type SimpleTableFilterType = 'text' | 'select' | 'number' | 'checkbox' | 'date'
export type SimpleTableCellOverflow = 'ellipsis' | 'wrap' | 'visible'

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

    /**
     * How a read-only cell handles text wider than the configured column.
     * Defaults to `ellipsis` so long values do not expand the table layout.
     */
    overflow?: SimpleTableCellOverflow

    enumOptions?: SelectOption[]

    /** Numeric editor constraints. They are also reflected in the browser input. */
    min?: number
    max?: number
    step?: number | string
    suffix?: string

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
