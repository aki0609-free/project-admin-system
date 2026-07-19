import { defineStore } from 'pinia'

type PaginationState = {
  page: number
  itemsPerPage: number
}

export const useMultiLevelHeaderPaginationStore = defineStore(
  'multiLevelHeaderPagination',
  {
    state: () => ({
      tables: {} as Record<string, PaginationState>,
    }),

    actions: {
      init(tableKey: string, defaultPerPage = 10) {
        if (!this.tables[tableKey]) {
          this.tables[tableKey] = {
            page: 1,
            itemsPerPage: defaultPerPage,
          }
        }
      },

      setPage(tableKey: string, page: number) {
        this.init(tableKey)
        this.tables[tableKey]!.page = page
      },

      setItemsPerPage(tableKey: string, size: number) {
        this.init(tableKey)
        this.tables[tableKey]!.itemsPerPage = size
        this.tables[tableKey]!.page = 1
      },

      reset(tableKey: string) {
        if (!this.tables[tableKey]) return
        this.tables[tableKey]!.page = 1
      },
    },
  }
)