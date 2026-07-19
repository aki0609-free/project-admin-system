import { computed } from 'vue'
import { useMultiLevelHeaderPaginationStore } from '../store/paging/useMultiLevelHeaderPaginationStore' 

export function useMultiLevelHeaderPagination(tableKey: string) {
  const store = useMultiLevelHeaderPaginationStore()

  store.init(tableKey)

  const state = computed(() => store.tables[tableKey]!)

  const page = computed({
    get: () => state.value.page,
    set: (v) => store.setPage(tableKey, v),
  })

  const itemsPerPage = computed({
    get: () => state.value.itemsPerPage,
    set: (v) => store.setItemsPerPage(tableKey, v),
  })

  return {
    page,
    itemsPerPage,
  }
}