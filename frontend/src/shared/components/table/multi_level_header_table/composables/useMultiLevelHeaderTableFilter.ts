import { computed } from 'vue'
import { useMultiLevelHeaderFilterStore } from '../store/filter/useMultiLevelHeaderFilterStore' 
import type { MultiLevelHeaderTableFilterRule } from '../types/filter/types'

export function useMultiLevelHeaderFilter(tableKey: string) {
  const store = useMultiLevelHeaderFilterStore()

  store.init(tableKey)

  const tableState = computed(() => store.tables[tableKey]!)

  const enabled = computed({
    get: () => tableState.value.enabled,
    set: (v: boolean) => store.setEnabled(tableKey, v),
  })

  const logic = computed({
    get: () => tableState.value.logic,
    set: (v) => store.setLogic(tableKey, v),
  })

  const filters = computed(() => tableState.value.filters)

  const setFilter = (key: string, value: unknown) => {
    store.setFilter(tableKey, key, value)
  }

  const reset = () => store.reset(tableKey)

  const applyFilter = <T>(
    items: T[],
    rules: MultiLevelHeaderTableFilterRule<T>[],
  ) => {
    return store.applyFilter(tableKey, items, rules)
  }

  return {
    enabled,
    logic,
    filters,
    setFilter,
    reset,
    applyFilter,
  }
}