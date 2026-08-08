import { useMutation } from '@tanstack/vue-query'
import { post } from '@/shared/api/http'
import type {
  MailTemplatePreviewRequest,
  MailTemplatePreviewResponse,
} from '@/features/system/mail/types/mailApiTypes'

export const usePreviewMailTemplateMutation = () =>
  useMutation({
    mutationFn: async (request: MailTemplatePreviewRequest) =>
      await post<MailTemplatePreviewResponse, MailTemplatePreviewRequest>(
        '/api/system/mail-templates/preview',
        request,
      ),
  })
