import { PivotFilterPredicate, PivotFilterType } from '../types/filter/types'

export const defaultPivotPredicates: {
  [K in PivotFilterType]: <T extends Record<string, any>>(key: keyof T) => PivotFilterPredicate<T>
} = {
  text: (key) => (item, value) => {
    if (typeof value !== 'string') return true

    if (value === '!') {
      const v = item[key]
      return v === null || String(v).trim() === ''
    }

    if (value === '*') {
      const v = item[key]
      return v !== null && String(v).trim() !== ''
    }

    return String(item[key] ?? '').includes(value)
  },

  select: (key) => (item, value) => {
    if (value == null || value === '') return true
    const itemValue = item[key]

    if (Array.isArray(value)) {
      return value.map(String).includes(String(itemValue))
    }

    return String(item[key]) === String(value)
  },

number:
  <T extends Record<string, any>>(key: keyof T) =>
  (item: T, value) => {
    if (value == null) return true

    const v = Number(item[key])
    return v === Number(value)
  },

  checkbox: (key) => (item, value) => {
    if (value === null) return true
    if (typeof value !== 'boolean') return true
    return item[key] === value
  },
}
