import { SimpleTableFilterRule, SimpleTableFilterType } from "../types/filter/types"
import { SimpleTableColumnDef } from "../types/item/types"
import { defaultSimpleTablePredicates } from "./defaultSimpleTablePredicates"
import { getSimpleTableColumnValue } from "./getSimpleTableColumnValue"

export const createSimpleTableFilterRules = <T>(
  columns: SimpleTableColumnDef<T>[]
): SimpleTableFilterRule<T>[] => {
  return columns
    .filter(
      (c): c is SimpleTableColumnDef<T> & { filter: NonNullable<typeof c.filter> } =>
        !!c.filter
    )
    .map((c) => {
      const key = c.key as keyof T

      const type = c.filter.type as SimpleTableFilterType
      const predicateFn = defaultSimpleTablePredicates[type]

      if (!predicateFn) {
        console.warn(`Unknown filter type "${c.filter.type}" for column`)
        return {
          key: String(key),
          predicate: () => true, 
        }
      }

      const getValue = (item: T) => getSimpleTableColumnValue(item, c)

      return {
        key: String(key),
        predicate:
          c.filter.predicate ??
          predicateFn(getValue),
      }
    })
}