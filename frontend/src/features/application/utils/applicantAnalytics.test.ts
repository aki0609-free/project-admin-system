import { describe, expect, it } from 'vitest'
import type { ApplicantRow } from '@/features/application/types/applicantTypes'
import { summarizeApplicants } from './applicantAnalytics'

function applicant(overrides: Partial<ApplicantRow>): ApplicantRow {
  return {
    id: 1,
    no: 1,
    name: '応募者',
    furiganaName: '',
    contractType: 'CONTRACT',
    retirementStatus: 'WORKING',
    birthDate: null,
    gender: 'MALE',
    joinDateWithoutInsurance: null,
    leaveDateWithoutInsurance: null,
    joinDateWithInsurance: null,
    leaveDateWithInsurance: null,
    contactDate: '2026-08-01',
    recruitmentStatus: 'HIRED',
    sourceWhere: '',
    searchSite: '',
    keyword: '',
    repostedTo: '',
    applicationReason1: '',
    applicationReason2: '',
    recruitmentCompany: '',
    mediaType: '',
    mediaName: '媒体A',
    mediaArea: '',
    mediaSlots: null,
    recruitmentUnitPrice: null,
    dailyWageAtJoin: null,
    needsDormitory: 'NO',
    roomType: 'UNKNOWN',
    dormitoryFee: null,
    previousJob: '',
    previousJobPeriod: '',
    insuredBefore: 'NO',
    dormitoryExperience: 'NO',
    previousDormitoryFee: null,
    clientBeforeLeave: '',
    industryBeforeLeave: '',
    estimatedRetirementReason: '',
    ...overrides,
  }
}

describe('summarizeApplicants', () => {
  it('総計・月別・媒体別を同じ判定規則で集計する', () => {
    const result = summarizeApplicants([
      applicant({ id: 1 }),
      applicant({
        id: 2,
        no: 2,
        contactDate: '2026-08-15',
        recruitmentStatus: 'INTERVIEW',
        retirementStatus: 'RESIGNED',
        gender: 'FEMALE',
      }),
      applicant({
        id: 3,
        no: 3,
        contactDate: '2026-09-01',
        recruitmentStatus: 'WITHDRAW',
        retirementStatus: 'UNKNOWN',
        mediaName: '媒体B',
      }),
    ])

    expect(result.totalApplicants).toBe(3)
    expect(result.interviewedCount).toBe(2)
    expect(result.workingCount).toBe(1)
    expect(result.resignedCount).toBe(1)
    expect(result.interviewRate).toBe(67)
    expect(result.monthlySummary[0]).toMatchObject({
      yearMonth: '2026-08',
      applicants: 2,
      interviewed: 2,
      working: 1,
      resigned: 1,
    })
    expect(result.mediaSummary).toEqual([
      { mediaName: '媒体A', applicants: 2, working: 1, resigned: 1, workingRate: 50 },
      { mediaName: '媒体B', applicants: 1, working: 0, resigned: 0, workingRate: 0 },
    ])
  })

  it('対象が0件でも割合を0として返す', () => {
    const result = summarizeApplicants([])
    expect(result.interviewRate).toBe(0)
    expect(result.workingRate).toBe(0)
    expect(result.monthlySummary).toEqual([])
  })
})
