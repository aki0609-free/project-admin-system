import type {
  PaymentCycle,
} from '@/features/employees/types/employeeWorkApiTypes'

const DAY_MILLISECONDS =
  24 * 60 * 60 * 1000

const toIsoDate = (date: Date): string =>
  date.toISOString().slice(0, 10)

const parseIsoDate = (
  value: string,
): Date | null => {
  const match =
    /^(\d{4})-(\d{2})-(\d{2})$/.exec(value)

  if (!match) return null

  const year = Number(match[1])
  const month = Number(match[2]) - 1
  const day = Number(match[3])
  const date = new Date(Date.UTC(year, month, day))

  return toIsoDate(date) === value
    ? date
    : null
}

const addDays = (
  date: Date,
  days: number,
): Date =>
  new Date(date.getTime() + days * DAY_MILLISECONDS)

const rollForwardFromSunday = (
  date: Date,
): Date =>
  date.getUTCDay() === 0
    ? addDays(date, 1)
    : date

/**
 * 新規日報の勤務日初期値。前日が日曜日の場合だけ土曜日へ戻す。
 * 土曜日と祝日は通常日として扱い、必要に応じて利用者が手修正する。
 */
export const calculateDefaultWorkDate = (
  currentBusinessDateValue: string,
): string => {
  const currentBusinessDate =
    parseIsoDate(currentBusinessDateValue)

  if (!currentBusinessDate) return ''

  let candidate = addDays(currentBusinessDate, -1)
  if (candidate.getUTCDay() === 0) {
    candidate = addDays(candidate, -1)
  }

  return toIsoDate(candidate)
}

export const calculateSuggestedPaymentDate = (
  workDateValue: string,
  paymentCycle: PaymentCycle | null | undefined,
): string => {
  const workDate = parseIsoDate(workDateValue)
  if (!workDate || !paymentCycle) return ''

  let candidate: Date

  switch (paymentCycle) {
    case 'DAILY':
      candidate = addDays(workDate, 1)
      break

    case 'WEEKLY': {
      const weekday = workDate.getUTCDay()
      const daysToFollowingMonday =
        weekday === 0 ? 1 : 8 - weekday
      candidate = addDays(workDate, daysToFollowingMonday)
      break
    }

    case 'MONTHLY':
      candidate = new Date(Date.UTC(
        workDate.getUTCFullYear(),
        workDate.getUTCMonth() + 1,
        15,
      ))
      break
  }

  return toIsoDate(
    rollForwardFromSunday(candidate),
  )
}
