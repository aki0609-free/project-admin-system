import { normalizeTimeHHmm } from '@/shared/utils/TimeUtils'

export type DailyReportTimeCalculationInput = {
  startTime: string
  endTime: string
  breakMinutes: number | null | undefined
}

export type DailyReportTimeCalculationResult = {
  workHours: number
  overtimeHours: number
  nightWorkHours: number
}

const MINUTES_PER_DAY = 24 * 60
const STANDARD_WORK_MINUTES = 8 * 60

const NIGHT_START_MINUTES = 22 * 60
const NIGHT_END_MINUTES = 5 * 60

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

  return {
    workHours: roundHours(workMinutes),
    overtimeHours:
      roundHours(overtimeMinutes),
    nightWorkHours:
      roundHours(nightMinutes),
  }
}