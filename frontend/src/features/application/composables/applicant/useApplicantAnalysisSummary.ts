import { computed, type Ref } from 'vue'
import type { ApplicantRow } from '@/features/application/types/applicantTypes'
import { summarizeApplicants } from '@/features/application/utils/applicantAnalytics'

export type ApplicantMediaSummaryItem = {
  mediaName: string
  count: number
}

export const useApplicantAnalysisSummary = (
  applicants: Ref<ApplicantRow[]>,
) => {
  const visibleApplicants = computed(() => applicants.value)
  const summary = computed(() => summarizeApplicants(visibleApplicants.value))
  const mediaSummary = computed<ApplicantMediaSummaryItem[]>(() =>
    summary.value.mediaSummary
      .map(item => ({ mediaName: item.mediaName, count: item.applicants }))
      .sort((a, b) => b.count - a.count || a.mediaName.localeCompare(b.mediaName)),
  )

  return {
    visibleApplicants,
    totalApplicants: computed(() => summary.value.totalApplicants),
    interviewedCount: computed(() => summary.value.interviewedCount),
    workingCount: computed(() => summary.value.workingCount),
    resignedCount: computed(() => summary.value.resignedCount),
    interviewRate: computed(() => summary.value.interviewRate),
    workingRate: computed(() => summary.value.workingRate),
    mediaSummary,
    monthlySummary: computed(() => summary.value.monthlySummary),
    majorMedia: computed(() => mediaSummary.value[0] ?? null),
  }
}
