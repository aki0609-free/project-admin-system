import type {
  SimpleTableFilterPredicate,
  SimpleTableFilterType,
} from '../types/filter/types'

type ValueGetter<T> = (item: T) => unknown

export const defaultSimpleTablePredicates: {
  [K in SimpleTableFilterType]: <T>(getValue: ValueGetter<T>) => SimpleTableFilterPredicate<T>
} = {
  text: (getValue) => (item, value) => {
    if (typeof value !== 'string') return true

    const currentValue = getValue(item)

    if (value === '!') {
      return currentValue == null || String(currentValue).trim() === ''
    }

    if (value === '*') {
      return currentValue != null && String(currentValue).trim() !== ''
    }

    return String(currentValue ?? '')
      .toLowerCase()
      .includes(value.toLowerCase())
  },

  select: (getValue) => (item, value) => {
    if (value == null || value === '') return true

    const currentValue = getValue(item)

    if (Array.isArray(value)) {
      if (value.length === 0) return true

      const selected = value.map(normalize)

      if (Array.isArray(currentValue)) {
        const currentValues = currentValue.map(normalize)
        return selected.some(v => currentValues.includes(v))
      }

      return selected.includes(normalize(currentValue))
    }

    if (Array.isArray(currentValue)) {
      return currentValue.map(normalize).includes(normalize(value))
    }

    return normalize(currentValue) === normalize(value)
  },

  number: (getValue) => (item, value) => {
    if (value == null || value === '') return true

    const currentValue = getValue(item)
    if (currentValue == null || currentValue === '') return false

    return Number(currentValue) === Number(value)
  },

  checkbox: (getValue) => (item, value) => {
    if (value == null || value === '') return true

    const currentValue = getValue(item)

    if (value === true || value === 'true') {
      return Boolean(currentValue) === true
    }

    if (value === false || value === 'false') {
      return Boolean(currentValue) === false
    }

    return true
  },

  date: (getValue) => (item, value) => {
    if (value == null || value === '') return true

    const currentValue = getValue(item)
    if (currentValue == null || currentValue === '') return false

    return normalizeDate(currentValue) === normalizeDate(value)
  },
}

function normalize(v: unknown): string {
  if (v == null) return ''

  if (typeof v === 'object') {
    const record = v as Record<string, unknown>

    if ('value' in record) return String(record.value)
    if ('name' in record) return String(record.name)
    if ('code' in record) return String(record.code)
  }

  return String(v)
}

function normalizeDate(v: unknown): string {
  if (v == null || v === '') return ''

  if (v instanceof Date) {
    return formatDateToIso(v)
  }

  const value = String(v)

  // すでに YYYY-MM-DD ならそのまま返す
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return value
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value

  return formatDateToIso(date)
}

function formatDateToIso(date: Date): string {
  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}