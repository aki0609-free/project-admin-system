import type {
  MultiLevelHeaderTableFilterPredicate,
  MultiLevelHeaderTableFilterType,
} from '../types/filter/types'

export const defaultMultiLevelHeaderPredicates: {
  [K in MultiLevelHeaderTableFilterType]: <T>(
    key: keyof T
  ) => MultiLevelHeaderTableFilterPredicate<T>
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
    return String(item[key]) === String(value)
  },

  number: (key) => (item, value) => {
    if (value == null || value === '') return true
    return Number(item[key]) === Number(value)
  },

  checkbox: (key) => (item, value) => {
    if (value === null) return true
    if (typeof value !== 'boolean') return true
    return item[key] === value
  },

  date: (key) => (item, value) => {
    if (!value) return true

    const itemDate = new Date(item[key] as any)
    const filterDate = new Date(value as any)

    return (
      itemDate.getFullYear() === filterDate.getFullYear() &&
      itemDate.getMonth() === filterDate.getMonth() &&
      itemDate.getDate() === filterDate.getDate()
    )
  },
}