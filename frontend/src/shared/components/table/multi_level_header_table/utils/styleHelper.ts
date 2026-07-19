import { MultiLevelHeaderLeafColumn } from "../types/item/types";

export function getHeaderStyle(col: any) {
    return {
        width: col.width ? `${col.width}px` : undefined,
        textAlign: col.align ?? 'center'
    }
}

export function getCellStyle(col: MultiLevelHeaderLeafColumn<any>) {
    return {
        width: col.width ? `${col.width}px` : undefined,
        textAlign: col.align ?? 'left'
    }
}