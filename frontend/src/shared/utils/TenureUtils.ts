function parseDate(value?: string | null): Date | null {
  if (!value) return null

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return null

  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

function diffDays(start: Date, end: Date): number {
  const msPerDay = 1000 * 60 * 60 * 24
  return Math.floor((end.getTime() - start.getTime()) / msPerDay)
}

export function formatTenureFromDates(
  startValue?: string | null,
  endValue?: string | null,
): string {
  const start = parseDate(startValue)
  const end = parseDate(endValue)

  if (!start || !end) return ''
  if (end < start) return ''

  const totalDays = diffDays(start, end)

  if (totalDays === 0) return '0日'

  const years = Math.floor(totalDays / 365)
  const remainingAfterYears = totalDays % 365
  const months = Math.floor(remainingAfterYears / 30)
  const days = remainingAfterYears % 30

  const parts: string[] = []

  if (years > 0) parts.push(`${years}年`)
  if (months > 0) parts.push(`${months}か月`)
  if (days > 0) parts.push(`${days}日`)

  return parts.join('')
}

export function formatCurrentTenure(
  joinDate?: string | null,
  leaveDate?: string | null,
): string {
  if (!joinDate) return ''

  const today = new Date()
  const todayString = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`

  return formatTenureFromDates(joinDate, leaveDate || todayString)
}