import { describe, expect, it } from 'vitest'
import { ref } from 'vue'

import {
  createDispatchRowsFromAssignments,
  useDailyPreparationDispatchTableConfig,
} from './useDailyPreparationDispatchTableConfig'
import type { DailyPreparationAssignmentTableRow } from './useDailyPreparationAssignmentTableConfig'

describe('useDailyPreparationDispatchTableConfig', () => {
  it('同じ現場の従業員が複数いても配車行は現場単位で1行にする', () => {
    const assignments = [10, 11].map(employeeId => ({
      id: employeeId,
      employeeId,
      customerId: 20,
      customerSiteId: 30,
      customerName: '顧客A',
      siteName: '現場A',
    })) as DailyPreparationAssignmentTableRow[]

    const rows = createDispatchRowsFromAssignments(
      assignments,
      [{
        id: 40,
        preparationId: 50,
        customerId: 20,
        customerSiteId: 30,
        customerName: '顧客A',
        siteName: '現場A',
        distanceFromCompanyKm: 12.5,
        vehicleCount: 3,
        otherAmount: 0,
        note: null,
      }],
    )

    expect(rows).toHaveLength(1)
    expect(rows[0]).toEqual(expect.objectContaining({
      customerSiteId: 30,
      vehicleCount: 3,
    }))
  })

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
      () => 12.5,
    )
    const { columns } = useDailyPreparationDispatchTableConfig(ref(rows))

    expect(rows[0]?.distanceFromCompanyKm).toBe(12.5)
    expect(rows[0]?.otherAmount).toBe(0)
    expect(columns.value.find(column => column.key === 'distanceFromCompanyKm')?.editable)
      .toBe(true)
    expect(columns.value.find(column => column.key === 'distanceFromCompanyKm')?.step)
      .toBe(0.1)
    expect(columns.value.find(column => column.key === 'otherAmount')?.editable)
      .toBe(true)
  })
})
