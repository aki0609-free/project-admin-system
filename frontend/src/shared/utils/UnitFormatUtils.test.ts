import { describe, expect, it } from 'vitest'

import {
  formatDecimalHoursAsHourMinute,
  formatMinutesAsHourMinute,
  formatNumberWithUnit,
  parseHourMinuteToDecimalHours,
  parseHourMinuteToMinutes,
} from './UnitFormatUtils'

describe('UnitFormatUtils', () => {
  it('数値へ単位を付ける', () => {
    expect(formatNumberWithUnit(30, '円/km')).toBe('30円/km')
    expect(formatNumberWithUnit(12000, '円')).toBe('12,000円')
    expect(formatNumberWithUnit(null, '日')).toBe('')
  })

  it('分と小数時間をH:mm形式へ変換する', () => {
    expect(formatMinutesAsHourMinute(90)).toBe('1:30')
    expect(formatDecimalHoursAsHourMinute(1.5)).toBe('1:30')
    expect(formatDecimalHoursAsHourMinute(8)).toBe('8:00')
  })

  it('H:mm入力を内部保持用の分へ変換する', () => {
    expect(parseHourMinuteToMinutes('1:30')).toBe(90)
    expect(parseHourMinuteToMinutes('0:05')).toBe(5)
    expect(parseHourMinuteToMinutes('90')).toBe(90)
    expect(parseHourMinuteToMinutes('1:60')).toBeNull()
  })

  it('H:mm入力を内部保持用の小数時間へ変換する', () => {
    expect(parseHourMinuteToDecimalHours('1:30')).toBe(1.5)
    expect(parseHourMinuteToDecimalHours('0:05')).toBeCloseTo(5 / 60)
    expect(parseHourMinuteToDecimalHours('1.5')).toBe(1.5)
    expect(parseHourMinuteToDecimalHours('1:60')).toBeNull()
  })
})
