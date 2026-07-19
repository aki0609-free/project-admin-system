import { defineStore } from 'pinia'

type SortState = {
  key: string | null
  order: 'asc' | 'desc' | null
}

export const useMultiLevelHeaderSortStore = defineStore('mlhSort', {
  state: () => ({
    tables: {} as Record<string, SortState>,
  }),

  actions: {
    init(tableKey: string) {
      if (!this.tables[tableKey]) {
        this.tables[tableKey] = {
          key: null,
          order: null,
        }
      }
    },

    toggleSort(tableKey: string, key: string) {
      this.init(tableKey)

      const state = this.tables[tableKey]!

      if (state.key !== key) {
        state.key = key
        state.order = 'asc'
      } else if (state.order === 'asc') {
        state.order = 'desc'
      } else if (state.order === 'desc') {
        state.key = null
        state.order = null
      } else {
        state.order = 'asc'
      }
    },
  },

  getters: {
    applySort:
      (state) =>
      <T>(tableKey: string, items: T[]) => {
        const s = state.tables[tableKey]
        if (!s || !s.key || !s.order) return items

        return [...items].sort((a: any, b: any) => {
          const av = a[s.key!]
          const bv = b[s.key!]

          if (av == null) return 1
          if (bv == null) return -1

          if (av > bv) return s.order === 'asc' ? 1 : -1
          if (av < bv) return s.order === 'asc' ? -1 : 1
          return 0
        })
      },
  },
})