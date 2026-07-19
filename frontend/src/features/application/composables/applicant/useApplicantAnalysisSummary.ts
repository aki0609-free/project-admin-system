import { computed, type Ref } from 'vue'
import type { ApplicantRow } from '@/features/application/types/applicantTypes'

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
    visibleApplicants.value.filter(item => item.recruitmentStatus === 'INTERVIEW').length,
  )

  const workingCount = computed(() =>
    visibleApplicants.value.filter(item => item.retirementStatus === 'WORKING').length,
  )

  const resignedCount = computed(() =>
    visibleApplicants.value.filter(
      item =>
        item.retirementStatus === 'RESIGNED' ||
        item.retirementStatus === 'BACKOUT',
    ).length,
  )

  const interviewRate = computed(() => {
    if (totalApplicants.value === 0) return 0
    return Math.round((interviewedCount.value / totalApplicants.value) * 100)
  })

  const workingRate = computed(() => {
    if (totalApplicants.value === 0) return 0
    return Math.round((workingCount.value / totalApplicants.value) * 100)
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
      if (item.recruitmentStatus === 'INTERVIEW') current.interviewed += 1
      if (item.retirementStatus === 'WORKING') current.working += 1
      if (
        item.retirementStatus === 'RESIGNED' ||
        item.retirementStatus === 'BACKOUT'
      ) {
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
          value.applicants > 0
            ? Math.round((value.interviewed / value.applicants) * 100)
            : 0,
        workingRate:
          value.applicants > 0
            ? Math.round((value.working / value.applicants) * 100)
            : 0,
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