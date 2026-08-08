import { describe, expect, it } from 'vitest'
import { calculateDailyReportWorkTimes } from './dailyReportTimeCalculator'

describe('calculateDailyReportWorkTimes', () => {
  it('土曜日の勤務を休日時間へ振り替える', () => {
    const result = calculateDailyReportWorkTimes({
      workDate: '2026-08-08',
      startTime: '08:00',
      endTime: '19:00',
      breakMinutes: 60,
    })

    expect(result).toEqual({
      workHours: 0,
      overtimeHours: 0,
      nightWorkHours: 0,
      holidayWorkHours: 10,
    })
  })

  it('平日は通常と残業に分ける', () => {
    const result = calculateDailyReportWorkTimes({
      workDate: '2026-08-10',
      startTime: '08:00',
      endTime: '19:00',
      breakMinutes: 60,
    })

    expect(result).toEqual({
      workHours: 8,
      overtimeHours: 2,
      nightWorkHours: 0,
      holidayWorkHours: 0,
    })
  })
})
