import { computed, type Ref } from 'vue'
import type { ApplicantRow } from '@/features/application/types/applicantTypes'
import { summarizeApplicants } from '@/features/application/utils/applicantAnalytics'

export const useApplicantChartSummary = (
  applicants: Ref<ApplicantRow[]>,
) => {
  const summary = computed(() => summarizeApplicants(applicants.value))

  return {
    monthlySummary: computed(() => summary.value.monthlySummary),
    mediaSummary: computed(() => summary.value.mediaSummary),
    statusSummary: computed(() => summary.value.statusSummary),
    genderSummary: computed(() => summary.value.genderSummary),
    contractTypeSummary: computed(() => summary.value.contractTypeSummary),
  }
}
