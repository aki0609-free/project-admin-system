import type {
  MultiLevelHeaderTableFilterRule,
  MultiLevelHeaderTableFilterType,
  MultiLevelHeaderTableFilterPredicate,
} from '../types/filter/types'
import type { MultiLevelHeaderLeafColumn } from '../types/item/types'
import { defaultMultiLevelHeaderPredicates } from './defaultMultiLevelHeaderPredicates'

type RowFilterDef<T> = {
  key: keyof T
  filter?: {
    type: MultiLevelHeaderTableFilterType
    predicate?: MultiLevelHeaderTableFilterPredicate<T>
  }
}

export const createMultiLevelHeaderFilterRules = <T>(
  columns: MultiLevelHeaderLeafColumn<T>[],
  rowDef?: RowFilterDef<T>
): MultiLevelHeaderTableFilterRule<T>[] => {
  /* =========================
     column rules
  ========================= */

  const columnRules: MultiLevelHeaderTableFilterRule<T>[] = columns
    .filter(
      (c): c is MultiLevelHeaderLeafColumn<T> & {
        filter: NonNullable<typeof c.filter>
      } => !!c.filter
    )
    .map((c) => {
      const key = c.key as keyof T
      const type = c.filter.type as MultiLevelHeaderTableFilterType

      const predicateFactory = defaultMultiLevelHeaderPredicates[type]

      if (!predicateFactory) {
        console.warn(`Unknown filter type "${type}"`)
        return {
          key,
          type,
          predicate: () => true,
        }
      }

      return {
        key,
        type,
        predicate:
          c.filter.predicate ??
          predicateFactory(key),
      }
    })

  /* =========================
     row rule（←追加部分）
  ========================= */

  let rowRule: MultiLevelHeaderTableFilterRule<T>[] = []

  if (rowDef?.filter) {
    const key = rowDef.key
    const type = rowDef.filter.type

    const predicateFactory = defaultMultiLevelHeaderPredicates[type]

    if (!predicateFactory) {
      console.warn(`Unknown filter type "${type}"`)
      rowRule = [
        {
          key,
          type,
          predicate: () => true,
        },
      ]
    } else {
      rowRule = [
        {
          key,
          type,
          predicate:
            rowDef.filter.predicate ??
            predicateFactory(key),
        },
      ]
    }
  }

  /* =========================
     merge
  ========================= */

  return [...rowRule, ...columnRules]
}