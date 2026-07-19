import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { put } from '@/shared/api/http'
import { queryKeys } from '@/features/system/mail/api/queryKeys'
import type {
  MailTemplateResponse,
  MailTemplateSaveRequest,
} from '@/features/system/mail/types/mailApiTypes'

type Payload = {
  id: number
  request: MailTemplateSaveRequest
}

export const useUpdateMailTemplateMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async ({ id, request }: Payload) =>
      await put<MailTemplateResponse, MailTemplateSaveRequest>(
        `/api/system/mail-templates/${id}`,
        request,
      ),
    onSuccess: async (_data: unknown, variables: Payload) => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.mail.templates.all,
      })
      await queryClient.invalidateQueries({
        queryKey: queryKeys.mail.templates.detail(variables.id),
      })
    },
  })
}
