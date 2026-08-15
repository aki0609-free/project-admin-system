import { describe, expect, it } from 'vitest'
import {
  hasCompletedInterview,
  isResigned,
  nextApplicantNumber,
  percentage,
} from './applicantMetrics'

describe('applicantMetrics', () => {
  it('面接後の採用・初日退職も面接実施済みとして数える', () => {
    expect(hasCompletedInterview('JUST_CONTACT')).toBe(false)
    expect(hasCompletedInterview('WITHDRAW')).toBe(false)
    expect(hasCompletedInterview('INTERVIEW')).toBe(true)
    expect(hasCompletedInterview('HIRED')).toBe(true)
    expect(hasCompletedInterview('BACKOUT')).toBe(true)
  })

  it('通常退職と初日退職を退職者として数える', () => {
    expect(isResigned('WORKING')).toBe(false)
    expect(isResigned('RESIGNED')).toBe(true)
    expect(isResigned('BACKOUT')).toBe(true)
  })

  it('割合を四捨五入し、分母0は0にする', () => {
    expect(percentage(2, 3)).toBe(67)
    expect(percentage(1, 0)).toBe(0)
  })

  it('削除による欠番があっても最大番号の次を採番する', () => {
    expect(nextApplicantNumber([])).toBe(1)
    expect(nextApplicantNumber([1, 3])).toBe(4)
    expect(nextApplicantNumber([0, Number.NaN, 5])).toBe(6)
  })
})
