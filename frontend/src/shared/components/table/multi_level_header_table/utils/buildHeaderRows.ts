import type { MultiLevelHeaderColumnDef } from "../types/item/types"

export function buildHeaderRows<T>(
  columns: MultiLevelHeaderColumnDef<T>[]
) {
  const rows: any[][] = []
  const maxDepth = getMaxDepth(columns)

  function walk(cols: MultiLevelHeaderColumnDef<T>[], depth = 0) {
    rows[depth] ||= []

    cols.forEach(col => {
      const hasChildren = !!col.subColumns?.length

      rows[depth]!.push({
        title: col.title,
        key: col.key,
        colspan: hasChildren ? countLeaf(col) : 1,
        rowspan: hasChildren ? 1 : maxDepth - depth,

        // 🔥 追加
        width: col.width,
        align: col.align,
        isGroup: hasChildren,
      })

      if (hasChildren) {
        walk(col.subColumns!, depth + 1)
      }
    })
  }

  walk(columns)

  return rows
}

function countLeaf<T>(col: MultiLevelHeaderColumnDef<T>): number {
  if (!col.subColumns?.length) return 1
  return col.subColumns.reduce((sum, c) => sum + countLeaf(c), 0)
}

function getMaxDepth<T>(cols: MultiLevelHeaderColumnDef<T>[], depth = 1): number {
  return Math.max(
    ...cols.map(c =>
      c.subColumns?.length
        ? getMaxDepth(c.subColumns, depth + 1)
        : depth
    )
  )
}