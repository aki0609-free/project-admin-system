import { MultiLevelHeaderLeafColumn } from "../types/item/types";

export function format<T>(col: MultiLevelHeaderLeafColumn<T>, row: T) {
    const v = row[col.key]
    return col.formatter ? col.formatter(v, row) : v
}