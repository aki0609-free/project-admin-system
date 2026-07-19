import { toRef, type Ref } from 'vue'
import type { ApplicantRow } from '@/features/application/types/applicantTypes'
import { useApplicantChartFilter } from './useApplicantChartFilter'
import { useApplicantChartSummary } from './useApplicantChartSummary'
import { useApplicantChartData } from './useApplicantChartData'
import { useApplicantChartOptions } from './useApplicantChartOptions'

export const useApplicantChart = (
  applicants: Ref<ApplicantRow[]>,
) => {
  const filterPart = useApplicantChartFilter(applicants)
  const summaryPart = useApplicantChartSummary(toRef(filterPart, 'filteredApplicants'))
  const dataPart = useApplicantChartData(
    summaryPart.monthlySummary,
    summaryPart.mediaSummary,
    summaryPart.statusSummary,
    summaryPart.genderSummary,
    summaryPart.contractTypeSummary,
  )
  const optionPart = useApplicantChartOptions()

  return {
    ...filterPart,
    ...summaryPart,
    ...dataPart,
    ...optionPart,
  }
}