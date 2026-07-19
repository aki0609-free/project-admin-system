import { computed } from 'vue'
import { useMultiLevelHeaderSortStore } from '../store/sort/useMultiLevelHeaderSortStore' 

export function useMultiLevelHeaderSort(tableKey: string) {
  const store = useMultiLevelHeaderSortStore()

  store.init(tableKey)

  const state = computed(() => store.tables[tableKey]!)

  const sortKey = computed(() => state.value.key)
  const sortOrder = computed(() => state.value.order)

  const toggleSort = (key: string) => {
    store.toggleSort(tableKey, key)
  }

  const applySort = <T>(items: T[]) => {
    return store.applySort(tableKey, items)
  }

  return {
    sortKey,
    sortOrder,
    toggleSort,
    applySort,
  }
}