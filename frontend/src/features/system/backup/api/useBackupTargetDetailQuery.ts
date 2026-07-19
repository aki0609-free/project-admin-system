import { computed, type ComputedRef } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/system/backup/api/queryKeys'
import type { BackupTargetResponse } from '@/features/system/backup/types/backupApiTypes'

export const useBackupTargetDetailQuery = (
  id: ComputedRef<number | null>,
) => {
  const query = useAppQuery({
    queryKey: computed(() => queryKeys.backup.detail(id.value)),
    enabled: computed(() => id.value != null),
    queryFn: async () =>
      await get<BackupTargetResponse>(`/api/system/backup/targets/${id.value}`),
  })

  const target = computed(() => query.data.value ?? null)

  return {
    ...query,
    target,
  }
}