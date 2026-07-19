// composables/useMultiLevelHeaderAggregation.ts

import { computed } from 'vue'
import type {
  MultiLevelHeaderLeafColumn,
  MultiLevelHeaderAggregation,
} from '../types/item/types'

export function useMultiLevelHeaderAggregation<T>(
  items: () => T[], // computedでも生配列でもOK
  columns: () => MultiLevelHeaderLeafColumn<T>[]
) {
  const summary = computed(() => {
    const result: Record<string, unknown> = {}

    for (const col of columns()) {
      const key = col.key as string

      if (!col.aggregation) {
        result[key] = ''
        continue
      }

      const values = items()
        .map((row) => row[col.key])
        .filter((v) => v != null)

      switch (col.aggregation) {
        case 'sum':
          result[key] = values.reduce(
            (sum, v) => sum + Number(v || 0),
            0
          )
          break

        case 'avg':
          result[key] =
            values.length === 0
              ? 0
              : values.reduce((sum, v) => sum + Number(v || 0), 0) /
                values.length
          break

        case 'count':
          result[key] = values.length
          break

        default:
          result[key] = ''
      }
    }

    return result
  })

  return {
    summary,
  }
}