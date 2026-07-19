import { computed, type ComputedRef } from 'vue'
import type { ApplicationMediaLocalItem } from '@/features/application/types/applicationMediaTypes'

export const useApplicationMediaChartSummary = (
  filteredMedias: ComputedRef<ApplicationMediaLocalItem[]>,
) => {
  const monthlySummary = computed(() => {
    const map = new Map<string, { cost: number; hires: number }>()

    for (const item of filteredMedias.value) {
      const ym = item.mediaYearMonth
      if (!ym) continue

      const current = map.get(ym) ?? { cost: 0, hires: 0 }
      current.cost += item.cost ?? 0
      current.hires += item.hires ?? 0
      map.set(ym, current)
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

  const mediaSummary = computed(() => {
    const map = new Map<string, { cost: number; hires: number }>()

    for (const item of filteredMedias.value) {
      const mediaName = item.mediaName
      if (!mediaName) continue

      const current = map.get(mediaName) ?? { cost: 0, hires: 0 }
      current.cost += item.cost ?? 0
      current.hires += item.hires ?? 0
      map.set(mediaName, current)
    }

    return Array.from(map.entries())
      .map(([mediaName, value]) => ({
        mediaName,
        cost: value.cost,
        hires: value.hires,
        unitPrice: value.hires > 0 ? Math.round(value.cost / value.hires) : 0,
      }))
      .sort((a, b) => a.mediaName.localeCompare(b.mediaName))
  })

  return {
    monthlySummary,
    mediaSummary,
  }
}