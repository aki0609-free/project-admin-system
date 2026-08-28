import type { ApplicationMediaLocalItem } from '@/features/application/types/applicationMediaTypes'

export type ApplicationMediaSummaryItem = {
  mediaName: string
  cost: number
  hires: number
  unitPrice: number
}

export type ApplicationMediaMonthlySummaryItem = {
  mediaYearMonth: string
  cost: number
  hires: number
  unitPrice: number
}

export type ApplicationMediaAnalyticsSummary = {
  totalCost: number
  totalHires: number
  averageUnitPrice: number
  mediaSummary: ApplicationMediaSummaryItem[]
  monthlySummary: ApplicationMediaMonthlySummaryItem[]
}

type Accumulator = { cost: number; hires: number }

function unitPrice(cost: number, hires: number): number {
  return hires > 0 ? Math.round(cost / hires) : 0
}

export function summarizeApplicationMedia(
  medias: readonly ApplicationMediaLocalItem[],
): ApplicationMediaAnalyticsSummary {
  const visibleMedias = medias.filter(item => !item.isDeleted)
  const byMedia = new Map<string, Accumulator>()
  const byMonth = new Map<string, Accumulator>()
  let totalCost = 0
  let totalHires = 0

  for (const item of visibleMedias) {
    const cost = item.cost ?? 0
    const hires = item.hires ?? 0
    totalCost += cost
    totalHires += hires

    if (item.mediaName) {
      const current = byMedia.get(item.mediaName) ?? { cost: 0, hires: 0 }
      current.cost += cost
      current.hires += hires
      byMedia.set(item.mediaName, current)
    }

    if (item.mediaYearMonth) {
      const current = byMonth.get(item.mediaYearMonth) ?? { cost: 0, hires: 0 }
      current.cost += cost
      current.hires += hires
      byMonth.set(item.mediaYearMonth, current)
    }
  }

  return {
    totalCost,
    totalHires,
    averageUnitPrice: unitPrice(totalCost, totalHires),
    mediaSummary: Array.from(byMedia.entries())
      .map(([mediaName, value]) => ({
        mediaName,
        ...value,
        unitPrice: unitPrice(value.cost, value.hires),
      }))
      .sort((a, b) => a.mediaName.localeCompare(b.mediaName)),
    monthlySummary: Array.from(byMonth.entries())
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([mediaYearMonth, value]) => ({
        mediaYearMonth,
        ...value,
        unitPrice: unitPrice(value.cost, value.hires),
      })),
  }
}
