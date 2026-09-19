import { describe, expect, it } from 'vitest'
import { ref } from 'vue'

import {
  createDispatchRowsFromAssignments,
  useDailyPreparationDispatchTableConfig,
} from './useDailyPreparationDispatchTableConfig'
import type { DailyPreparationAssignmentTableRow } from './useDailyPreparationAssignmentTableConfig'

describe('useDailyPreparationDispatchTableConfig', () => {
  it('現場マスタの距離を初期値にし、距離とその他金額を編集できる', () => {
    const assignment = {
      id: 10,
      employeeId: 10,
      customerId: 20,
      customerSiteId: 30,
      customerName: '顧客A',
      siteName: '現場A',
    } as DailyPreparationAssignmentTableRow

    const rows = createDispatchRowsFromAssignments(
      [assignment],
      [],
      [],
      () => 12,
    )
    const { columns } = useDailyPreparationDispatchTableConfig(ref(rows))

    expect(rows[0]?.distanceFromCompanyKm).toBe(12)
    expect(rows[0]?.otherAmount).toBe(0)
    expect(columns.value.find(column => column.key === 'distanceFromCompanyKm')?.editable)
      .toBe(true)
    expect(columns.value.find(column => column.key === 'otherAmount')?.editable)
      .toBe(true)
  })
})
