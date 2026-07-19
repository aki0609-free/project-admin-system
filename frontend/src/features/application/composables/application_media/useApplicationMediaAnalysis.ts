import { computed, type Ref } from 'vue'
import type { ApplicationMediaLocalItem } from '@/features/application/types/applicationMediaTypes'
import { useApplicationMediaAnalysisSummary } from './useApplicationMediaAnalysisSummary'
// import { useApplicationMediaAiAnalysisMutation } from '@/features/application/api/useApplicationMediaAiAnalysisMutation'

export const useApplicationMediaAnalysis = (
  medias: Ref<ApplicationMediaLocalItem[]>,
) => {
  const summary = useApplicationMediaAnalysisSummary(medias)

  const analysisComments = computed(() => {
    const comments: string[] = []

    if (summary.bestEfficiencyMedia.value) {
      comments.push(
        `最も効率が良い媒体は「${summary.bestEfficiencyMedia.value.mediaName}」で、平均単価は ${summary.bestEfficiencyMedia.value.unitPrice.toLocaleString()}円です。`,
      )
    }

    if (summary.highestCostMedia.value) {
      comments.push(
        `最もコストが高い媒体は「${summary.highestCostMedia.value.mediaName}」で、総コストは ${summary.highestCostMedia.value.cost.toLocaleString()}円です。`,
      )
    }

    if (summary.topHiresMedia.value) {
      comments.push(
        `採用数が最も多い媒体は「${summary.topHiresMedia.value.mediaName}」で、総採用数は ${summary.topHiresMedia.value.hires}件です。`,
      )
    }

    if (summary.averageUnitPrice.value > 0) {
      comments.push(
        `全体の平均単価は ${summary.averageUnitPrice.value.toLocaleString()}円です。高コスト媒体と高採用媒体のバランス確認が重要です。`,
      )
    }

    return comments
  })

  const canAnalyzeByAi = computed(() => summary.mediaSummary.value.length > 0)

  const aiAnalysisEnabled = false

  const analyzeByAi = async () => {
    // 将来ここで使う payload
    // const aiPayload = computed(() => ({
    //   totalCost: summary.totalCost.value,
    //   totalHires: summary.totalHires.value,
    //   averageUnitPrice: summary.averageUnitPrice.value,
    //   mediaSummary: summary.mediaSummary.value,
    //   monthlySummary: summary.monthlySummary.value,
    // }))
    //
    // const aiAnalysisMutation = useApplicationMediaAiAnalysisMutation()
    // return await aiAnalysisMutation.mutateAsync(aiPayload.value)

    console.info('AI分析はまだ未接続です')
  }

  return {
    ...summary,
    analysisComments,
    canAnalyzeByAi,
    aiAnalysisEnabled,
    analyzeByAi,
  }
}