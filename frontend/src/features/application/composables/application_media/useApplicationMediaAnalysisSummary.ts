import { computed, type Ref } from 'vue'
import type { ApplicationMediaLocalItem } from '@/features/application/types/applicationMediaTypes'

export type ApplicationMediaSummaryItem = {
  mediaName: string
  cost: number
  hires: number
  unitPrice: number
}

export const useApplicationMediaAnalysisSummary = (medias: Ref<ApplicationMediaLocalItem[]>) => {
  const visibleMedias = computed(() => medias.value.filter((item) => !item.isDeleted))

  const totalCost = computed(() =>
    visibleMedias.value.reduce((sum, item) => sum + (item.cost ?? 0), 0),
  )

  const totalHires = computed(() =>
    visibleMedias.value.reduce((sum, item) => sum + (item.hires ?? 0), 0),
  )

  const averageUnitPrice = computed(() => {
    if (totalHires.value === 0) return 0
    return Math.round(totalCost.value / totalHires.value)
  })

  const mediaSummary = computed<ApplicationMediaSummaryItem[]>(() => {
    const map = new Map<string, { cost: number; hires: number }>()

    for (const item of visibleMedias.value) {
      const mediaName = item.mediaName
      if (!mediaName) continue

      const current = map.get(mediaName) ?? { cost: 0, hires: 0 }
      current.cost += item.cost ?? 0
      current.hires += item.hires ?? 0
      map.set(mediaName, current)
    }

    return Array.from(map.entries()).map(([mediaName, value]) => ({
      mediaName,
      cost: value.cost,
      hires: value.hires,
      unitPrice: value.hires > 0 ? Math.round(value.cost / value.hires) : 0,
    }))
  })

  const bestEfficiencyMedia = computed(() => {
    return (
      [...mediaSummary.value]
        .filter((item) => item.hires > 0)
        .sort((a, b) => a.unitPrice - b.unitPrice)[0] ?? null
    )
  })

  const highestCostMedia = computed(() => {
    return [...mediaSummary.value].sort((a, b) => b.cost - a.cost)[0] ?? null
  })

  const topHiresMedia = computed(() => {
    return [...mediaSummary.value].sort((a, b) => b.hires - a.hires)[0] ?? null
  })

  const monthlySummary = computed(() => {
    const map = new Map<string, { cost: number; hires: number }>()

    for (const item of visibleMedias.value) {
      const mediaYearMonth = item.mediaYearMonth
      if (!mediaYearMonth) continue

      const current = map.get(mediaYearMonth) ?? { cost: 0, hires: 0 }
      current.cost += item.cost ?? 0
      current.hires += item.hires ?? 0
      map.set(mediaYearMonth, current)
    }

    return Array.from(map.entries())
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([mediaYearMonth, value]) => ({
        mediaYearMonth,
        cost: value.cost,
        hires: value.hires,
        unitPrice: value.hires > 0 ? Math.round(value.cost / value.hires) : 0,
      }))
  })

  return {
    visibleMedias,
    totalCost,
    totalHires,
    averageUnitPrice,
    mediaSummary,
    monthlySummary,
    bestEfficiencyMedia,
    highestCostMedia,
    topHiresMedia,
  }
}
