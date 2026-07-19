export function useRowGrouping<T>(
    items: T[],
    groupBy: keyof T
) {
    const map = new Map<string, T[]>()

    for (const item of items) {
        const key = String(item[groupBy] ?? '未分類')

        if (!map.has(key)) {
            map.set(key, [])
        }
        map.get(key)!.push(item)
    }

    return map
}