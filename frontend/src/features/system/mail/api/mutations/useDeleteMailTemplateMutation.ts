import { useQueryClient } from '@tanstack/vue-query'
import { useAppMutation } from '@/shared/api/useAppMutation'
import { del } from '@/shared/api/http'
import { queryKeys } from '@/features/system/mail/api/queryKeys'

export const useDeleteMailTemplateMutation = () => {
  const queryClient = useQueryClient()

  return useAppMutation({
    mutationFn: async ({ id }: { id: number }) =>
      await del<unknown>(`/api/system/mail-templates/${id}`),
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.mail.templates.all,
      })
    },
  })
}
