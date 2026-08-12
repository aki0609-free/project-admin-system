import { normalizeTimeHHmm } from '@/shared/utils/TimeUtils'

export type DailyReportTimeCalculationInput = {
  workDate: string
  startTime: string
  endTime: string
  breakMinutes: number | null | undefined
  holidayPremiumEligible: boolean
}

export type DailyReportTimeCalculationResult = {
  workHours: number
  overtimeHours: number
  nightWorkHours: number
  holidayWorkHours: number
}

const MINUTES_PER_DAY = 24 * 60
const STANDARD_WORK_MINUTES = 8 * 60

const NIGHT_START_MINUTES = 22 * 60
const NIGHT_END_MINUTES = 5 * 60

export function isWeekendDate(value: string): boolean {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value)
  if (!match) {
    return false
  }

  const date = new Date(
    Number(match[1]),
    Number(match[2]) - 1,
    Number(match[3]),
  )
  const day = date.getDay()
  return day === 0 || day === 6
}

function parseTimeToMinutes(
  value: string,
): number | null {
  if (!value) {
    return null
  }

  const normalized = normalizeTimeHHmm(value)

  const [
    hourText,
    minuteText,
  ] = normalized.split(':')

  const hour = Number(hourText)
  const minute = Number(minuteText)

  if (
    Number.isNaN(hour)
    || Number.isNaN(minute)
  ) {
    return null
  }

  return hour * 60 + minute
}

function roundHours(
  minutes: number,
): number {
  return Math.round(
    (minutes / 60) * 100,
  ) / 100
}

function calculateNightMinutes(
  startMinutes: number,
  endMinutes: number,
): number {
  let total = 0

  for (
    let current = startMinutes;
    current < endMinutes;
    current += 1
  ) {
    const minuteOfDay =
      current % MINUTES_PER_DAY

    const isNight =
      minuteOfDay >= NIGHT_START_MINUTES
      || minuteOfDay < NIGHT_END_MINUTES

    if (isNight) {
      total += 1
    }
  }

  return total
}

export function calculateDailyReportWorkTimes(
  input: DailyReportTimeCalculationInput,
): DailyReportTimeCalculationResult | null {
  const startMinutes =
    parseTimeToMinutes(input.startTime)

  const rawEndMinutes =
    parseTimeToMinutes(input.endTime)

  if (
    startMinutes == null
    || rawEndMinutes == null
  ) {
    return null
  }

  let endMinutes = rawEndMinutes

  if (endMinutes <= startMinutes) {
    endMinutes += MINUTES_PER_DAY
  }

  const breakMinutes = Math.max(
    Number(input.breakMinutes ?? 0),
    0,
  )

  const totalMinutes = Math.max(
    endMinutes
      - startMinutes
      - breakMinutes,
    0,
  )

  const workMinutes = Math.min(
    totalMinutes,
    STANDARD_WORK_MINUTES,
  )

  const overtimeMinutes = Math.max(
    totalMinutes
      - STANDARD_WORK_MINUTES,
    0,
  )

  const nightMinutes =
    calculateNightMinutes(
      startMinutes,
      endMinutes,
    )

  const workHours = roundHours(workMinutes)
  const overtimeHours = roundHours(overtimeMinutes)
  const holidayPremiumEligible =
    input.holidayPremiumEligible

  return {
    workHours: holidayPremiumEligible ? 0 : workHours,
    overtimeHours: holidayPremiumEligible ? 0 : overtimeHours,
    nightWorkHours:
      roundHours(nightMinutes),
    holidayWorkHours: holidayPremiumEligible
      ? roundHours(totalMinutes)
      : 0,
  }
}
