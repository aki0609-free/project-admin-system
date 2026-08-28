import { computed, type ComputedRef } from 'vue'
import type { ApplicationMediaLocalItem } from '@/features/application/types/applicationMediaTypes'
import { summarizeApplicationMedia } from '@/features/application/utils/applicationMediaAnalytics'

export const useApplicationMediaChartSummary = (
  filteredMedias: ComputedRef<ApplicationMediaLocalItem[]>,
) => {
  const summary = computed(() => summarizeApplicationMedia(filteredMedias.value))

  return {
    monthlySummary: computed(() => summary.value.monthlySummary),
    mediaSummary: computed(() => summary.value.mediaSummary),
  }
}
