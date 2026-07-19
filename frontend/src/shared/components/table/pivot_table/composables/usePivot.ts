import { PivotOptions } from "../types/pivot/types"

export function usePivot<T extends Record<string, any>>(
  data: T[],
  options: PivotOptions<T>
) {
  const {
    rowKey,
    columnKey,
    valueKey,
    aggregation = 'sum',
    rowFormatter = v => String(v),
    columnFormatter = v => String(v),
    valueFormatter = v => String(v),
    filter,
    sortRows,
    sortColumns,
    showRowTotals = false,
    showColumnTotals = false,
  } = options

  const filtered = filter ? data.filter(filter) : data

  const rowSet = new Set<string>()
  const colSet = new Set<string>()

  filtered.forEach(item => {
    rowSet.add(rowFormatter(item[rowKey]))
    colSet.add(columnFormatter(item[columnKey]))
  })

  const rowKeys = Array.from(rowSet)
  const columnKeys = Array.from(colSet)

  if (sortRows) rowKeys.sort(sortRows)
  if (sortColumns) columnKeys.sort(sortColumns)

  const bucket: Record<string, number[]> = {}

  filtered.forEach(item => {
    const r = rowFormatter(item[rowKey])
    const c = columnFormatter(item[columnKey])
    const key = `${r}__${c}`

    if (!bucket[key]) bucket[key] = []

    const value = Number(item[valueKey])
    if (!isNaN(value)) bucket[key].push(value)
  })

  const aggregate = (arr: number[]): number => {
    if (!arr.length) return 0

    switch (aggregation) {
      case 'sum':
        return arr.reduce((a, b) => a + b, 0)
      case 'avg':
        return arr.reduce((a, b) => a + b, 0) / arr.length
      case 'count':
        return arr.length
      case 'min':
        return Math.min(...arr)
      case 'max':
        return Math.max(...arr)
    }
  }

  const matrix = rowKeys.map(r =>
    columnKeys.map(c => {
      const key = `${r}__${c}`
      return aggregate(bucket[key] || [])
    })
  )

  const aggregateRow = (row: number[], rIndex: number) => {
    if (!row.length) return 0

    switch (aggregation) {
      case 'avg': {
        const values = columnKeys.flatMap(c => {
          const key = `${rowKeys[rIndex]}__${c}`
          return bucket[key] || []
        })
        return values.length
          ? values.reduce((a, b) => a + b, 0) / values.length
          : 0
      }

      case 'count': {
        return columnKeys.reduce((sum, c) => {
          const key = `${rowKeys[rIndex]}__${c}`
          return sum + (bucket[key]?.length ?? 0)
        }, 0)
      }

      default:
        return row.reduce((a, b) => a + b, 0)
    }
  }

  const rowTotals = matrix.map((row, i) => aggregateRow(row, i))

  const columnTotals = columnKeys.map((c, colIndex) => {
    switch (aggregation) {
      case 'avg': {
        const values = rowKeys.flatMap(r => {
          const key = `${r}__${c}`
          return bucket[key] || []
        })
        return values.length
          ? values.reduce((a, b) => a + b, 0) / values.length
          : 0
      }

      case 'count': {
        return rowKeys.reduce((sum, r) => {
          const key = `${r}__${c}`
          return sum + (bucket[key]?.length ?? 0)
        }, 0)
      }

      default:
        return matrix.reduce((sum, row) => sum + (row[colIndex] ?? 0), 0)
    }
  })

  const grandTotal = (() => {
    switch (aggregation) {
      case 'avg': {
        const all = Object.values(bucket).flat()
        return all.length
          ? all.reduce((a, b) => a + b, 0) / all.length
          : 0
      }

      case 'count':
        return Object.values(bucket).reduce((sum, arr) => sum + arr.length, 0)

      default:
        return rowTotals.reduce((a, b) => a + b, 0)
    }
  })()

  return {
    rowKeys,
    columnKeys,
    matrix,

    rowTotals,
    columnTotals,
    grandTotal,

    formattedMatrix: matrix.map(row => row.map(valueFormatter)),
    formattedRowTotals: rowTotals.map(valueFormatter),
    formattedColumnTotals: columnTotals.map(valueFormatter),
    formattedGrandTotal: valueFormatter(grandTotal),

    showRowTotals,
    showColumnTotals,
  }
}