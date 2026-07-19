import type { SimpleTableColumnDef } from "../types/item/types"

export const getSimpleTableColumnValue = <T>(
  row: T,
  column: SimpleTableColumnDef<T>,
): unknown => {
  if (column.valueGetter) {
    return column.valueGetter(row)
  }

  return row[column.key as keyof T]
}