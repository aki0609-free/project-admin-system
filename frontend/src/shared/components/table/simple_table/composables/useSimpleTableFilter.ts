/* eslint-disable @typescript-eslint/no-non-null-assertion */
import { computed } from "vue";
import { useSimpleTableFilterStore } from "../store/filter/useSimpleTableFilterStore";
import type { SimpleTableFilterRule } from "../types/filter/types";

export function useSimpleTableFilter(tableKey: string) {
    const store = useSimpleTableFilterStore()

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

    const applyFilter = <T>(items: T[], rules: SimpleTableFilterRule<T>[]) => {
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