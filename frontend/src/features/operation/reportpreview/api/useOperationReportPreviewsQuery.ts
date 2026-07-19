import { computed, unref, type MaybeRef } from 'vue'
import { get } from '@/shared/api/http'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { operationReportPreviewQueryKeys } from './queryKeys'
import type {
  OperationReportPreviewResponse,
  OperationType,
} from '../types/operationReportPreviewTypes'

export const useOperationReportPreviewsQuery = (
  operationType: MaybeRef<OperationType>,
) => {
  const query = useAppQuery<OperationReportPreviewResponse[]>({
    queryKey: computed(() =>
      operationReportPreviewQueryKeys.byOperationType(unref(operationType)),
    ),

    enabled: computed(() => !!unref(operationType)),

    queryFn: async () =>
      await get<OperationReportPreviewResponse[]>(
        '/api/operation/report-previews',
        {
          params: {
            query: {
              operationType: unref(operationType),
            },
          },
        },
      ),
  })

  return {
    ...query,
    reports: computed(() => query.data.value ?? []),
  }
}