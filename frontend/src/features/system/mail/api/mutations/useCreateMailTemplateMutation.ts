import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import { queryKeys } from '@/features/system/mail/api/queryKeys'
import type {
  MailTemplateResponse,
  MailTemplateSaveRequest,
} from '@/features/system/mail/types/mailApiTypes'

export const useCreateMailTemplateMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async (request: MailTemplateSaveRequest) =>
      await post<MailTemplateResponse, MailTemplateSaveRequest>(
        '/api/system/mail-templates',
        request,
      ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.mail.templates.all,
      })
    },
  })
}
