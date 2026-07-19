import { computed } from "vue"
import { usePivotTableFilterStore } from "../store/filter/usePivotTableFilterStore"
import { PivotFilterRule } from "../types/filter/types"

export function usePivotTableFilter(tableKey: string) {
  const store = usePivotTableFilterStore()

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

  const rowFilters = computed(() => tableState.value.rowFilters)
  const columnFilters = computed(() => tableState.value.columnFilters)
  const valueFilters = computed(() => tableState.value.valueFilters)

  const setFilter = (
    axis: 'row' | 'column' | 'value',
    key: string,
    value: unknown
  ) => {
    store.setFilter(tableKey, axis, key, value)
  }

  const reset = () => store.reset(tableKey)

  const applyFilter = <T>(
    items: T[],
    rules: PivotFilterRule<T>[]
  ) => {
    return store.applyFilter(tableKey, items, rules)
  }

  return {
    enabled,
    logic,

    rowFilters,
    columnFilters,
    valueFilters,

    setFilter,
    reset,
    applyFilter,
  }
}