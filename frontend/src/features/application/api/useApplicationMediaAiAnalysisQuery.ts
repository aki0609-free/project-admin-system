import { computed, type Ref } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { postApplicationMediaAiAnalysis } from '@/features/application/api/postApplicationMediaAiAnalysis'
import { queryKeys } from '@/features/application/api/queryKeys'
import type {
  AiAnalysisResponse,
  ApplicationMediaAiAnalysisRequest,
} from '@/features/application/types/applicationMediaAiTypes'

export const useApplicationMediaAiAnalysisQuery = (
  payload: Ref<ApplicationMediaAiAnalysisRequest>,
  enabled: Ref<boolean>,
) => {
  const queryKey = computed(() =>
    queryKeys.applicationMediaAi.analysis(payload.value),
  )

  return useQuery<AiAnalysisResponse>({
    queryKey: queryKey.value,
    queryFn: () => postApplicationMediaAiAnalysis(payload.value),
    enabled: enabled.value,
  })
}