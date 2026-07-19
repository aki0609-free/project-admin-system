import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import type {
  MailTemplatePreviewRequest,
  MailTemplatePreviewResponse,
} from '@/features/system/mail/types/mailApiTypes'

export const usePreviewMailTemplateMutation = () =>
  useAppMutation({
    mutationFn: async (request: MailTemplatePreviewRequest) =>
      await post<MailTemplatePreviewResponse, MailTemplatePreviewRequest>(
        '/api/system/mail-templates/preview',
        request,
      ),
  })
