import type { Ref } from 'vue'
import type { ApplicationMediaLocalItem } from '@/features/application/types/applicationMediaTypes'
import { useApplicationMediaChartFilter } from './useApplicationMediaChartFilter'
import { useApplicationMediaChartSummary } from './useApplicationMediaChartSummary'
import { useApplicationMediaChartData } from './useApplicationMediaChartData'

export const useApplicationMediaChart = (
  medias: Ref<ApplicationMediaLocalItem[]>,
) => {
  const filterPart = useApplicationMediaChartFilter(medias)
  const summaryPart = useApplicationMediaChartSummary(filterPart.filteredMedias)
  const dataPart = useApplicationMediaChartData(
    summaryPart.monthlySummary,
    summaryPart.mediaSummary,
  )

  return {
    ...filterPart,
    ...summaryPart,
    ...dataPart,
  }
}