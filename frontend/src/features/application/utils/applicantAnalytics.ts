import {
  applicantContractTypeLabelMap,
  applicantGenderLabelMap,
  applicantRetirementStatusLabelMap,
} from '@/features/application/constants/applicantConstants'
import type { ApplicantRow } from '@/features/application/types/applicantTypes'
import {
  hasCompletedInterview,
  isResigned,
  isWorking,
  percentage,
} from '@/features/application/utils/applicantMetrics'

export type ApplicantMonthlySummary = {
  yearMonth: string
  applicants: number
  interviewed: number
  working: number
  resigned: number
  interviewRate: number
  workingRate: number
}

export type ApplicantMediaSummary = {
  mediaName: string
  applicants: number
  working: number
  resigned: number
  workingRate: number
}

export type ApplicantRatioSummary = {
  label: string
  count: number
}

export type ApplicantAnalyticsSummary = {
  totalApplicants: number
  interviewedCount: number
  workingCount: number
  resignedCount: number
  interviewRate: number
  workingRate: number
  monthlySummary: ApplicantMonthlySummary[]
  mediaSummary: ApplicantMediaSummary[]
  statusSummary: ApplicantRatioSummary[]
  genderSummary: ApplicantRatioSummary[]
  contractTypeSummary: ApplicantRatioSummary[]
}

type MonthlyAccumulator = {
  applicants: number
  interviewed: number
  working: number
  resigned: number
}

type MediaAccumulator = {
  applicants: number
  working: number
  resigned: number
}

function increment(map: Map<string, number>, key: string | null | undefined) {
  if (!key) return
  map.set(key, (map.get(key) ?? 0) + 1)
}

function toRatioSummary(
  map: Map<string, number>,
  labels: Readonly<Record<string, string>>,
): ApplicantRatioSummary[] {
  return Array.from(map.entries()).map(([value, count]) => ({
    label: labels[value] ?? value,
    count,
  }))
}

export function summarizeApplicants(
  applicants: readonly ApplicantRow[],
): ApplicantAnalyticsSummary {
  const monthly = new Map<string, MonthlyAccumulator>()
  const media = new Map<string, MediaAccumulator>()
  const statuses = new Map<string, number>()
  const genders = new Map<string, number>()
  const contractTypes = new Map<string, number>()

  let interviewedCount = 0
  let workingCount = 0
  let resignedCount = 0

  for (const applicant of applicants) {
    const interviewed = hasCompletedInterview(applicant.recruitmentStatus)
    const working = isWorking(applicant.retirementStatus)
    const resigned = isResigned(applicant.retirementStatus)

    if (interviewed) interviewedCount += 1
    if (working) workingCount += 1
    if (resigned) resignedCount += 1

    const yearMonth = applicant.contactDate?.slice(0, 7)
    if (yearMonth) {
      const current = monthly.get(yearMonth) ?? {
        applicants: 0,
        interviewed: 0,
        working: 0,
        resigned: 0,
      }
      current.applicants += 1
      if (interviewed) current.interviewed += 1
      if (working) current.working += 1
      if (resigned) current.resigned += 1
      monthly.set(yearMonth, current)
    }

    if (applicant.mediaName) {
      const current = media.get(applicant.mediaName) ?? {
        applicants: 0,
        working: 0,
        resigned: 0,
      }
      current.applicants += 1
      if (working) current.working += 1
      if (resigned) current.resigned += 1
      media.set(applicant.mediaName, current)
    }

    increment(statuses, applicant.retirementStatus)
    increment(genders, applicant.gender)
    increment(contractTypes, applicant.contractType)
  }

  const totalApplicants = applicants.length

  return {
    totalApplicants,
    interviewedCount,
    workingCount,
    resignedCount,
    interviewRate: percentage(interviewedCount, totalApplicants),
    workingRate: percentage(workingCount, totalApplicants),
    monthlySummary: Array.from(monthly.entries())
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([yearMonth, value]) => ({
        yearMonth,
        ...value,
        interviewRate: percentage(value.interviewed, value.applicants),
        workingRate: percentage(value.working, value.applicants),
      })),
    mediaSummary: Array.from(media.entries())
      .map(([mediaName, value]) => ({
        mediaName,
        ...value,
        workingRate: percentage(value.working, value.applicants),
      }))
      .sort((a, b) => a.mediaName.localeCompare(b.mediaName)),
    statusSummary: toRatioSummary(statuses, applicantRetirementStatusLabelMap),
    genderSummary: toRatioSummary(genders, applicantGenderLabelMap),
    contractTypeSummary: toRatioSummary(contractTypes, applicantContractTypeLabelMap),
  }
}
