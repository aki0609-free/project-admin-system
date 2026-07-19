import { MultiLevelHeaderColumnDef, MultiLevelHeaderLeafColumn } from "../types/item/types";

export function isLeaf<T>(
  col: MultiLevelHeaderColumnDef<T>
): col is MultiLevelHeaderLeafColumn<T> {
  return !col.subColumns
}