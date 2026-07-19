import type {
  MultiLevelHeaderColumnDef,
  MultiLevelHeaderLeafColumn,
} from "../types/item/types"
import { isLeaf } from "./isLeaf" 

export function getLeafColumns<T>(
  columns: MultiLevelHeaderColumnDef<T>[]
) {
  const result: MultiLevelHeaderLeafColumn<T>[] = []

  function walk(cols: MultiLevelHeaderColumnDef<T>[]) {
    cols.forEach(col => {
      if (col.subColumns?.length) {
        walk(col.subColumns)
      } else if (isLeaf(col)) {
        result.push(col)
      }
    })
  }

  walk(columns)
  return result
}