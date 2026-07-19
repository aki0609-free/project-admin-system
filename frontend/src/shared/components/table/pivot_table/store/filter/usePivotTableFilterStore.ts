import { defineStore } from "pinia";
import { PivotFilterLogic, PivotFilterRule } from "../../types/filter/types";

type PivotFilterState = {
    enabled: boolean
    logic: PivotFilterLogic

    rowFilters: Record<string, unknown>
    columnFilters: Record<string, unknown>
    valueFilters: Record<string, unknown>
}

export const usePivotTableFilterStore = defineStore('pivotTableFilter', {
    state: () => ({
        tables: {} as Record<string, PivotFilterState>,
    }),

    actions: {
        init(tableKey: string) {
            if (!this.tables[tableKey]) {
                this.tables[tableKey] = {
                    enabled: true,
                    logic: 'AND',
                    rowFilters: {},
                    columnFilters: {},
                    valueFilters: {},
                }
            }
        },

        setFilter(
            tableKey: string,
            axis: 'row' | 'column' | 'value',
            key: string,
            value: unknown
        ) {
            this.init(tableKey)
            this.tables[tableKey]![`${axis}Filters`][key] = value
        },

        reset(tableKey: string) {
            if (!this.tables[tableKey]) return

            this.tables[tableKey]!.rowFilters = {}
            this.tables[tableKey]!.columnFilters = {}
            this.tables[tableKey]!.valueFilters = {}
        },

        setLogic(tableKey: string, logic: PivotFilterLogic) {
            this.init(tableKey)
            this.tables[tableKey]!.logic = logic
        },

        setEnabled(tableKey: string, enabled: boolean) {
            this.init(tableKey)
            this.tables[tableKey]!.enabled = enabled
        }

    },

    getters: {
        applyFilter:
            (state) =>
                <T>(
                    tableKey: string,
                    items: T[],
                    rules: PivotFilterRule<T>[]
                ): T[] => {
                    const table = state.tables[tableKey]
                    if (!table || !table.enabled) return items

                    return items.filter((item) => {
                        const conditions = rules.map((rule) => {
                            const axis = rule.axis ?? 'value'

                            const filterValue = table[`${axis}Filters`][String(rule.key)]

                            if (filterValue === undefined || filterValue === null || filterValue === '') {
                                return true
                            }

                            return rule.predicate(item, filterValue)
                        })

                        return table.logic === 'AND'
                            ? conditions.every(Boolean)
                            : conditions.some(Boolean)
                    })
                },
    },
})