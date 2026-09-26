export const formatNumberWithUnit = (
  value: unknown,
  unit: string,
): string => {
  if (value == null || value === '') return ''

  const numberValue = Number(value)
  if (!Number.isFinite(numberValue)) return String(value)

  return `${numberValue.toLocaleString('ja-JP')}${unit}`
}

export const formatMinutesAsHourMinute = (value: unknown): string => {
  if (value == null || value === '') return ''

  const minutes = Number(value)
  if (!Number.isFinite(minutes)) return String(value)

  return formatTotalMinutes(Math.round(minutes))
}

export const formatDecimalHoursAsHourMinute = (value: unknown): string => {
  if (value == null || value === '') return ''

  const hours = Number(value)
  if (!Number.isFinite(hours)) return String(value)

  return formatTotalMinutes(Math.round(hours * 60))
}

export const parseHourMinuteToMinutes = (value: unknown): number | null => {
  if (value == null || value === '') return null
  if (typeof value === 'number') {
    return Number.isFinite(value) && value >= 0
      ? Math.round(value)
      : null
  }

  const text = String(value).trim()
  if (/^\d+$/.test(text)) return Number(text)

  const match = /^(\d+):([0-5]\d)$/.exec(text)
  if (!match) return null

  return Number(match[1]) * 60 + Number(match[2])
}

export const parseHourMinuteToDecimalHours = (
  value: unknown,
): number | null => {
  if (value == null || value === '') return null
  if (typeof value === 'number') {
    return Number.isFinite(value) && value >= 0
      ? value
      : null
  }

  const text = String(value).trim()
  if (/^\d+(?:\.\d+)?$/.test(text)) return Number(text)

  const match = /^(\d+):([0-5]\d)$/.exec(text)
  if (!match) return null

  return (
    Number(match[1])
    + Number(match[2]) / 60
  )
}

const formatTotalMinutes = (totalMinutes: number): string => {
  const sign = totalMinutes < 0 ? '-' : ''
  const absoluteMinutes = Math.abs(totalMinutes)
  const hours = Math.floor(absoluteMinutes / 60)
  const minutes = absoluteMinutes % 60

  return `${sign}${hours}:${String(minutes).padStart(2, '0')}`
}
