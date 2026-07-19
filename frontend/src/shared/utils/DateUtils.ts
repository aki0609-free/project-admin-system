export const yearOptions = () => {
    const currentYear = new Date().getFullYear()

    return Array.from({ length: 5 }, (_, i) => {
        const year = currentYear -2 + i
        return {
            label: `${year}年`,
            value: String(year),
        }
    })
}

export const monthOptions = Array.from({ length: 12 }, (_, i) => ({
    label: `${i + 1}月`,
    value: String(i + 1),
}))

export function formatYearMonth(value?: string | null): string {
  if (!value) return ''

  const parts = value.split('-')
  if (parts.length !== 2) return value

  const [year, month] = value.split('-')
  if (!year || !month) return value
  return `${year}年${Number(month)}月`
}

export function formatYearMonthDay(value?: string | null): string {
  if (!value) return ''

  const parts = value.split('-')
  if (parts.length !== 3) return value

  const [year, month, day] = parts
  if (!year || !month || !day) return value

  return `${year}年${Number(month)}月${Number(day)}日`
}

export function normalizeYearMonth(year: string, month: string) {
    return `${year}-${month.padStart(2, '0')}`
}

export function sortYearMonth(values: string[]) {
    return [...values].sort((a, b) => a.localeCompare(b))
}

export function calculateAgeAtDate(
  birthDate: string | null | undefined,
  baseDate: string | null | undefined,
): number | null {
  if (!birthDate || !baseDate) return null

  const birth = new Date(birthDate)
  const base = new Date(baseDate)

  if (Number.isNaN(birth.getTime()) || Number.isNaN(base.getTime())) {
    return null
  }

  let age = base.getFullYear() - birth.getFullYear()

  const birthMonthDay = `${String(birth.getMonth() + 1).padStart(2, '0')}${String(birth.getDate()).padStart(2, '0')}`
  const baseMonthDay = `${String(base.getMonth() + 1).padStart(2, '0')}${String(base.getDate()).padStart(2, '0')}`

  if (baseMonthDay < birthMonthDay) {
    age -= 1
  }

  return age
}

export function normalizeDateValue(value: unknown): string {
  if (!value) return ''

  if (Array.isArray(value)) {
    return value[0] ? String(value[0]) : ''
  }

  if (value instanceof Date) {
    return formatDateToIso(value)
  }

  return String(value)
}

export function formatDateToIso(date: Date): string {
  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

export function toYearMonth(value: string | null | undefined): string | null {
  if (!value) return null

  return value.slice(0, 7)
}