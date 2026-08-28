import { computed, type Ref } from 'vue'
import type { ApplicationMediaLocalItem } from '@/features/application/types/applicationMediaTypes'
import {
  summarizeApplicationMedia,
  type ApplicationMediaSummaryItem,
} from '@/features/application/utils/applicationMediaAnalytics'

export type { ApplicationMediaSummaryItem }

export const useApplicationMediaAnalysisSummary = (
  medias: Ref<ApplicationMediaLocalItem[]>,
) => {
  const visibleMedias = computed(() => medias.value.filter(item => !item.isDeleted))
  const summary = computed(() => summarizeApplicationMedia(visibleMedias.value))

  const bestEfficiencyMedia = computed(() =>
    [...summary.value.mediaSummary]
      .filter(item => item.hires > 0)
      .sort((a, b) => a.unitPrice - b.unitPrice)[0] ?? null,
  )

  const highestCostMedia = computed(() =>
    [...summary.value.mediaSummary].sort((a, b) => b.cost - a.cost)[0] ?? null,
  )

  const topHiresMedia = computed(() =>
    [...summary.value.mediaSummary].sort((a, b) => b.hires - a.hires)[0] ?? null,
  )

  return {
    visibleMedias,
    totalCost: computed(() => summary.value.totalCost),
    totalHires: computed(() => summary.value.totalHires),
    averageUnitPrice: computed(() => summary.value.averageUnitPrice),
    mediaSummary: computed<ApplicationMediaSummaryItem[]>(() => summary.value.mediaSummary),
    monthlySummary: computed(() => summary.value.monthlySummary),
    bestEfficiencyMedia,
    highestCostMedia,
    topHiresMedia,
  }
}
