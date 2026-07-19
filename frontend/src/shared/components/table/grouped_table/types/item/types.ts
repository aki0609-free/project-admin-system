export type GroupedTableDef<T> = {
    rowKey: keyof T
    columns: {
        key: keyof T
        title: string
    }[]
    groupBy: keyof T
}