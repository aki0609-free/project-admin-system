import { defineStore } from 'pinia'
import type { SimpleTableFilterLogic, SimpleTableFilterRule } from '../../types/filter/types'

type FilterState = {
  enabled: boolean
  logic: SimpleTableFilterLogic
  filters: Record<string, unknown>
}

export const useSimpleTableFilterStore = defineStore('simpleTableFilter', {
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

    setLogic(tableKey: string, logic: SimpleTableFilterLogic) {
      this.init(tableKey)
      this.tables[tableKey]!.logic = logic
    },

    setEnabled(tableKey: string, enabled: boolean) {
      this.init(tableKey)
      this.tables[tableKey]!.enabled = enabled
    },

    setFilter(tableKey: string, key: string, value: unknown) {
      this.init(tableKey)
      this.tables[tableKey]!.filters[key] = value
    },
  },
  getters: {
    applyFilter:
      (state) =>
      <T>(tableKey: string, items: T[], rules: SimpleTableFilterRule<T>[]) => {
        const table = state.tables[tableKey]
        if (!table || !table.enabled) return items

        return items.filter((item) => {
          const conditions = rules
            .filter((r) => table.filters[r.key] != null && table.filters[r.key] != '')
            .map((r) => r.predicate(item, table.filters[r.key]))

          if (!conditions.length) return true

          return table.logic === 'AND' ? conditions.every(Boolean) : conditions.some(Boolean)
        })
      },
  },
})
