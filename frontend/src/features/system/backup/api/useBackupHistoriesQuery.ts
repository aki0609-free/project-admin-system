import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import { queryKeys } from '@/features/system/backup/api/queryKeys'
import type { BackupHistoryResponse } from '@/features/system/backup/types/backupApiTypes'

export const useBackupHistoriesQuery = () => {
  const query = useAppQuery({
    queryKey: queryKeys.backup.histories,
    queryFn: async () =>
      await get<BackupHistoryResponse[]>('/api/system/backup/histories'),
  })

  return {
    ...query,
    histories: computed(() => query.data.value ?? []),
  }
}