import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { ref } from 'vue'
import { useCustomerMasterStore } from '@/features/customer/store/useCustomerMasterStore'
import {
  useDailyPreparationAssignmentTableConfig,
  type DailyPreparationAssignmentTableRow,
} from './useDailyPreparationAssignmentTableConfig'

describe('useDailyPreparationAssignmentTableConfig', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('現場候補を行で選択中の顧客に所属する現場だけに絞る', () => {
    const customerStore = useCustomerMasterStore()
    customerStore.sites = [
      { id: 101, customerId: 1, name: '顧客A現場', distanceFromCompanyKm: 5 },
      { id: 201, customerId: 2, name: '顧客B現場', distanceFromCompanyKm: 8 },
    ]

    const row = {
      id: 1,
      customerId: 1,
    } as DailyPreparationAssignmentTableRow
    const { columns } = useDailyPreparationAssignmentTableConfig(ref([row]))
    const siteColumn = columns.value.find((column) => column.key === 'customerSiteId')

    expect(siteColumn).toBeDefined()
    expect(typeof siteColumn?.enumOptions).toBe('function')

    const options = typeof siteColumn?.enumOptions === 'function'
      ? siteColumn.enumOptions(row)
      : siteColumn?.enumOptions

    expect(options).toEqual([
      { title: '顧客A現場', value: 101 },
    ])
  })
})
