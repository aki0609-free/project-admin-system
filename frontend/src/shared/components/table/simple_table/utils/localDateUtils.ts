import { normalizeDateValue } from "@/shared/utils/DateUtils"

export function formatDatePickerHeader(date: unknown) {
  const value = normalizeDateValue(date)
  if (!value) return '日付を選択'

  const match = value.match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (!match) return '日付を選択'

  const [, year, month, day] = match
  return `${year}年${Number(month)}月${Number(day)}日`
}

export function formatDatePickerMonth(date: unknown) {
  const value = normalizeDateValue(date)
  if (!value) return ''

  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ''

  return `${d.getMonth() + 1}月`
}