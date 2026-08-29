export const normalizeTimeHHmm = (
  value: string | null | undefined,
): string => {
  if (!value) return ''

  const text = String(value).trim()
  if (!text) return ''

  // 09:00:00 → 09:00
  const hhmmssMatch = text.match(/^(\d{1,2}):(\d{2}):\d{2}$/)
  if (hhmmssMatch) {
    const hours = hhmmssMatch[1]
    const minutes = hhmmssMatch[2]
    if (hours && minutes) {
      return `${hours.padStart(2, '0')}:${minutes}`
    }
  }

  // 9:00 → 09:00
  const hhmmMatch = text.match(/^(\d{1,2}):(\d{2})$/)
  if (hhmmMatch) {
    const hours = hhmmMatch[1]
    const minutes = hhmmMatch[2]
    if (hours && minutes) {
      return `${hours.padStart(2, '0')}:${minutes}`
    }
  }

  return text
}

export const formatTimeHHmm = (
  value: unknown,
): string => {
  if (value == null) return ''
  return normalizeTimeHHmm(String(value))
}

export const formatTimeRange = (
  start: string | null | undefined,
  end: string | null | undefined,
): string => {
  const startText = normalizeTimeHHmm(start)
  const endText = normalizeTimeHHmm(end)

  if (!startText && !endText) return ''
  if (startText && !endText) return `${startText} -`
  if (!startText && endText) return `- ${endText}`

  return `${startText} - ${endText}`
}

export function formatHours(value: number | string): string {
  if (value === '') {
    return '';
  }

  const stringValue = String(value);
  if (stringValue.endsWith('時間')) {
    return stringValue;
  }

  return `${stringValue}時間`;
}
