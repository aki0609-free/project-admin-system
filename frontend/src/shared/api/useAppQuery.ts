import { useQuery, type UseQueryOptions } from '@tanstack/vue-query'

export function useAppQuery<TQueryFnData, TError = unknown, TData = TQueryFnData>(
  options: UseQueryOptions<TQueryFnData, TError, TData>,
) {
  return useQuery(options)
}