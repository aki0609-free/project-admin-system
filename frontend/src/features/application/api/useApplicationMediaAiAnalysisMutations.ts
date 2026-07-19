import { useAppMutation } from '@/shared/api/useAppMutation'
import { postApplicationMediaAiAnalysis } from '@/features/application/api/postApplicationMediaAiAnalysis'
import type {
  ApplicationMediaAiAnalysisRequest,
} from '@/features/application/types/applicationMediaAiTypes'

export const useApplicationMediaAiAnalysisMutation = () => {
  return useAppMutation({
    mutationFn: (payload: ApplicationMediaAiAnalysisRequest) =>
      postApplicationMediaAiAnalysis(payload),
  })
}