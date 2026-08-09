import { computed } from 'vue'
import { useAppQuery } from '@/shared/api/useAppQuery'
import { get } from '@/shared/api/http'
import type { EmployeePayrollItemSetting } from '../types/employeeApiTypes'
import { queryKeys } from './queryKeys'

export const useEmployeePayrollItemSettingCatalogQuery = () => {
  const query = useAppQuery<EmployeePayrollItemSetting[]>({
    queryKey: queryKeys.employees.payrollItemSettingCatalog(),
    queryFn: () => get<EmployeePayrollItemSetting[]>('/api/employees/payroll-item-settings/catalog'),
  })
  return { ...query, settings: computed(() => query.data.value ?? []) }
}
