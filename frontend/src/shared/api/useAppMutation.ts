import { useMutation } from '@tanstack/vue-query'

export function useAppMutation(options: any) {
  return useMutation({
    ...options,
    onError: (error: any, variables: any, context: any) => {
      console.log(error)
      options?.onError?.(error, variables, context)
    },
  })
}