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

/**
 * 指定タイムゾーン上の日付を、API入力用のyyyy-MM-ddへ変換する。
 * toISOString()はUTC日付になるため、日本時間の午前中に前日を
 * 初期表示してしまう画面では使用しない。
 */
export function formatDateInTimeZone(
  date: Date,
  timeZone = 'Asia/Tokyo',
): string {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(date)

  const year = parts.find(part => part.type === 'year')?.value
  const month = parts.find(part => part.type === 'month')?.value
  const day = parts.find(part => part.type === 'day')?.value

  if (!year || !month || !day) {
    throw new Error('日付の生成に失敗しました。')
  }

  return `${year}-${month}-${day}`
}

/**
 * 業務画面の初期日付を指定日数だけ移動して返す。
 *
 * Date#setDate は実行環境のローカルタイムゾーンに依存するため、
 * 基準時刻そのものを日単位で移動してから業務タイムゾーンの日付へ変換する。
 */
export function businessDateWithOffset(
  dayOffset: number,
  now = new Date(),
  timeZone = 'Asia/Tokyo',
): string {
  const shifted = new Date(
    now.getTime() + dayOffset * 24 * 60 * 60 * 1000,
  )
  return formatDateInTimeZone(shifted, timeZone)
}

export function businessMonthWithOffset(
  dayOffset: number,
  now = new Date(),
  timeZone = 'Asia/Tokyo',
): string {
  return businessDateWithOffset(dayOffset, now, timeZone).slice(0, 7)
}

export function toYearMonth(value: string | null | undefined): string | null {
  if (!value) return null

  return value.slice(0, 7)
}
