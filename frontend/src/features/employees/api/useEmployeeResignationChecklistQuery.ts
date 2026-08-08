import { computed } from 'vue'
import { get } from '@/shared/api/http'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { queryKeys } from './queryKeys'
import type { EmployeeResignationConfigurationResponse } from '../types/employeeApiTypes'

export const useEmployeeResignationChecklistQuery = () => {
  const query = useAppQuery({
    queryKey: queryKeys.employees.resignationConfiguration(),
    queryFn: async () =>
      await get<EmployeeResignationConfigurationResponse>(
        '/api/employees/resignation-configuration',
      ),
  })

  return {
    ...query,
    checklist: computed(() => query.data.value?.checklist ?? []),
    message: computed(() => query.data.value?.message ?? {
      dialogTitle: '退職処理',
      guidanceMessage: '退職日と確認項目を確認してから退職処理を実行してください。',
      confirmationMessage: '実行すると従業員の在籍状態が退職になります。',
    }),
  }
}
