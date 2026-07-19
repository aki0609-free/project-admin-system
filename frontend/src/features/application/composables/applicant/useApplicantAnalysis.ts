import { computed, type Ref } from 'vue'
import type { ApplicantRow } from '@/features/application/types/applicantTypes'
import { useApplicantAnalysisSummary } from './useApplicantAnalysisSummary'
// import { useApplicantAiAnalysisMutation } from '@/features/application/api/useApplicantAiAnalysisMutation'

export const useApplicantAnalysis = (
  applicants: Ref<ApplicantRow[]>,
) => {
  const summary = useApplicantAnalysisSummary(applicants)

  const analysisComments = computed(() => {
    const comments: string[] = []

    comments.push(`総応募者数は ${summary.totalApplicants.value} 名です。`)
    comments.push(
      `面接実施数は ${summary.interviewedCount.value} 名で、面接率は ${summary.interviewRate.value}% です。`,
    )
    comments.push(
      `在籍中人数は ${summary.workingCount.value} 名で、在籍率は ${summary.workingRate.value}% です。`,
    )
    comments.push(`退職人数は ${summary.resignedCount.value} 名です。`)

    if (summary.majorMedia.value) {
      comments.push(
        `最も応募数が多い媒体は「${summary.majorMedia.value.mediaName}」で、${summary.majorMedia.value.count} 名です。`,
      )
    }

    comments.push(
      '媒体別応募数と在籍率をあわせて確認すると、採用効率や定着しやすい媒体を把握しやすくなります。',
    )

    return comments
  })

  const canAnalyzeByAi = computed(() => summary.totalApplicants.value > 0)

  const aiAnalysisEnabled = false

  const analyzeByAi = async () => {
    // 将来ここで使う payload
    // const aiPayload = {
    //   totalApplicants: summary.totalApplicants.value,
    //   interviewedCount: summary.interviewedCount.value,
    //   workingCount: summary.workingCount.value,
    //   resignedCount: summary.resignedCount.value,
    //   interviewRate: summary.interviewRate.value,
    //   workingRate: summary.workingRate.value,
    //   mediaSummary: summary.mediaSummary.value,
    //   monthlySummary: summary.monthlySummary.value,
    // }
    //
    // const aiAnalysisMutation = useApplicantAiAnalysisMutation()
    // return await aiAnalysisMutation.mutateAsync(aiPayload)

    console.info('Applicant AI分析はまだ未接続です')
  }

  return {
    ...summary,
    analysisComments,
    canAnalyzeByAi,
    aiAnalysisEnabled,
    analyzeByAi,
  }
}