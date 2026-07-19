import { defineStore } from 'pinia'
import type {
  MultiLevelHeaderTableFilterLogic,
  MultiLevelHeaderTableFilterRule,
} from '../../types/filter/types'

type FilterState = {
  enabled: boolean
  logic: MultiLevelHeaderTableFilterLogic
  filters: Record<string, unknown>
}

export const useMultiLevelHeaderFilterStore = defineStore('mlhFilter', {
  state: () => ({
    tables: {} as Record<string, FilterState>,
  }),

  actions: {
    init(tableKey: string) {
      if (!this.tables[tableKey]) {
        this.tables[tableKey] = {
          enabled: true,
          logic: 'AND',
          filters: {},
        }
      }
    },

    reset(tableKey: string) {
      if (!this.tables[tableKey]) return
      this.tables[tableKey].filters = {}
    },

    setLogic(tableKey: string, logic: MultiLevelHeaderTableFilterLogic) {
      this.init(tableKey)
      this.tables[tableKey]!.logic = logic
    },

    setEnabled(tableKey: string, enabled: boolean) {
      this.init(tableKey)
      this.tables[tableKey]!.enabled = enabled
    },

    setFilter(tableKey: string, key: string, value: unknown) {
      this.init(tableKey)

      // 空は削除（UX大事）
      if (value == null || value === '') {
        delete this.tables[tableKey]!.filters[key]
      } else {
        this.tables[tableKey]!.filters[key] = value
      }
    },
  },

  getters: {
    applyFilter:
      (state) =>
      <T>(
        tableKey: string,
        items: T[],
        rules: MultiLevelHeaderTableFilterRule<T>[],
      ) => {
        const table = state.tables[tableKey]
        if (!table || !table.enabled) return items

        return items.filter((item) => {
          const conditions = rules
            .filter((r) => table.filters[r.key as string] != null)
            .map((r) =>
              r.predicate(item, table.filters[r.key as string]),
            )

          if (!conditions.length) return true

          return table.logic === 'AND'
            ? conditions.every(Boolean)
            : conditions.some(Boolean)
        })
      },
  },
})