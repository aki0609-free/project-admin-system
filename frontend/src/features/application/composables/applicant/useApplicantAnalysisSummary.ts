import { computed, type Ref } from 'vue'
import type { ApplicantRow } from '@/features/application/types/applicantTypes'
import {
  hasCompletedInterview,
  isResigned,
  isWorking,
  percentage,
} from '@/features/application/utils/applicantMetrics'

export type ApplicantMediaSummaryItem = {
  mediaName: string
  count: number
}

export const useApplicantAnalysisSummary = (
  applicants: Ref<ApplicantRow[]>,
) => {
  const visibleApplicants = computed(() => applicants.value)

  const totalApplicants = computed(() => visibleApplicants.value.length)

  const interviewedCount = computed(() =>
    visibleApplicants.value.filter(item =>
      hasCompletedInterview(item.recruitmentStatus),
    ).length,
  )

  const workingCount = computed(() =>
    visibleApplicants.value.filter(item => isWorking(item.retirementStatus)).length,
  )

  const resignedCount = computed(() =>
    visibleApplicants.value.filter(item => isResigned(item.retirementStatus)).length,
  )

  const interviewRate = computed(() => {
    return percentage(interviewedCount.value, totalApplicants.value)
  })

  const workingRate = computed(() => {
    return percentage(workingCount.value, totalApplicants.value)
  })

  const mediaSummary = computed<ApplicantMediaSummaryItem[]>(() => {
    const map = new Map<string, number>()

    for (const item of visibleApplicants.value) {
      if (!item.mediaName) continue
      map.set(item.mediaName, (map.get(item.mediaName) ?? 0) + 1)
    }

    return Array.from(map.entries())
      .map(([mediaName, count]) => ({
        mediaName,
        count,
      }))
      .sort((a, b) => b.count - a.count)
  })

  const majorMedia = computed(() => mediaSummary.value[0] ?? null)

  const monthlySummary = computed(() => {
    const map = new Map<
      string,
      {
        applicants: number
        interviewed: number
        working: number
        resigned: number
      }
    >()

    for (const item of visibleApplicants.value) {
      const yearMonth = item.contactDate?.slice(0, 7)
      if (!yearMonth) continue

      const current = map.get(yearMonth) ?? {
        applicants: 0,
        interviewed: 0,
        working: 0,
        resigned: 0,
      }

      current.applicants += 1
      if (hasCompletedInterview(item.recruitmentStatus)) current.interviewed += 1
      if (isWorking(item.retirementStatus)) current.working += 1
      if (isResigned(item.retirementStatus)) {
        current.resigned += 1
      }

      map.set(yearMonth, current)
    }

    return Array.from(map.entries())
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([yearMonth, value]) => ({
        yearMonth,
        ...value,
        interviewRate:
          percentage(value.interviewed, value.applicants),
        workingRate:
          percentage(value.working, value.applicants),
      }))
  })

  return {
    visibleApplicants,
    totalApplicants,
    interviewedCount,
    workingCount,
    resignedCount,
    interviewRate,
    workingRate,
    mediaSummary,
    monthlySummary,
    majorMedia,
  }
}
