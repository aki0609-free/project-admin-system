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

export type ApplicationMediaAiAnalysisRequest = {
  totalCost: number
  totalHires: number
  averageUnitPrice: number
  mediaSummary: ApplicationMediaSummaryItem[]
  monthlySummary?: ApplicationMediaMonthlySummaryItem[]
}

export type AiAnalysisResponse = {
  summary: string
  comments: string[]
  risks: string[]
  recommendations: string[]
  model: string
  promptVersion: string
}