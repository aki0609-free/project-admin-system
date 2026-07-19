import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/system/backup/api/queryKeys'
import type { BackupTargetResponse } from '@/features/system/backup/types/backupApiTypes'

export const useBackupTargetsQuery = () => {
  const query = useAppQuery({
    queryKey: queryKeys.backup.targets,
    queryFn: async () =>
      await get<BackupTargetResponse[]>('/api/system/backup/targets'),
  })

  const targets = computed(() => query.data.value ?? [])

  return {
    ...query,
    targets,
  }
}