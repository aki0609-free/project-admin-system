import { post } from '@/shared/api/http'
import type {
  AiAnalysisResponse,
  ApplicationMediaAiAnalysisRequest,
} from '@/features/application/types/applicationMediaAiTypes'

export const postApplicationMediaAiAnalysis = (
  payload: ApplicationMediaAiAnalysisRequest,
) => {
  return post<AiAnalysisResponse, ApplicationMediaAiAnalysisRequest>(
    '/api/application-media/analysis',
    payload,
  )
}