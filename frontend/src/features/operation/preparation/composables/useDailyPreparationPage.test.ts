import { describe, expect, it } from 'vitest'

import type { DailyPreparationAssignmentTableRow } from './useDailyPreparationAssignmentTableConfig'
import type { DailyPreparationDispatchTableRow } from './useDailyPreparationDispatchTableConfig'
import {
  buildAssignmentSaveItems,
  buildDispatchSaveItems,
  resetAssignmentRowValues,
} from './useDailyPreparationPage'

describe('useDailyPreparationPage save snapshots', () => {
  it('初回ヘッダー作成後に画面行が再構築されても従業員配置の保存内容を保持する', () => {
    const row = {
      assignmentId: null,
      employeeId: 10,
      customerId: 20,
      customerSiteId: 30,
      vehicleArrangementType: 'EMPLOYEE',
      passengerCount: 2,
      workDescription: '資材搬入',
      _isNew: true,
      _isUpdated: false,
      _isDeleted: false,
    } as DailyPreparationAssignmentTableRow

    const items = buildAssignmentSaveItems([row])

    row.customerId = null
    row.customerSiteId = null
    row._isNew = false

    expect(items).toEqual([
      expect.objectContaining({
        id: null,
        employeeId: 10,
        customerId: 20,
        customerSiteId: 30,
        vehicleArrangementType: 'EMPLOYEE',
        passengerCount: 2,
        workDescription: '資材搬入',
        isNew: true,
      }),
    ])
  })

  it('初回ヘッダー作成前に現場配車の保存内容も固定する', () => {
    const row = {
      dispatchId: null,
      customerId: 20,
      customerSiteId: 30,
      distanceFromCompanyKm: 18,
      vehicleCount: 1,
      otherAmount: -500,
      note: '調整',
      _isNew: true,
      _isUpdated: false,
      _isDeleted: false,
    } as DailyPreparationDispatchTableRow

    const items = buildDispatchSaveItems([row])

    row.otherAmount = 0
    row._isNew = false

    expect(items).toEqual([
      expect.objectContaining({
        id: null,
        customerId: 20,
        customerSiteId: 30,
        distanceFromCompanyKm: 18,
        vehicleCount: 1,
        otherAmount: -500,
        note: '調整',
        isNew: true,
      }),
    ])
  })

  it('保存済みの従業員配置は初期化後に削除対象になる', () => {
    const row = {
      assignmentId: 99,
      employeeId: 10,
      customerId: 20,
      customerSiteId: 30,
      customerName: '顧客A',
      siteName: '現場A',
      vehicleArrangementType: 'EMPLOYEE',
      passengerCount: 2,
      workDescription: '資材搬入',
      _isNew: false,
      _isUpdated: false,
      _isDeleted: false,
    } as DailyPreparationAssignmentTableRow

    resetAssignmentRowValues(row)

    expect(row).toEqual(expect.objectContaining({
      customerId: null,
      customerSiteId: null,
      customerName: '',
      siteName: '',
      vehicleArrangementType: 'NONE',
      passengerCount: 0,
      workDescription: '',
      _isDeleted: true,
    }))
    expect(buildAssignmentSaveItems([row])).toEqual([
      expect.objectContaining({ id: 99, isDeleted: true }),
    ])
  })

  it('未保存の従業員配置は初期化後に保存対象から外れる', () => {
    const row = {
      assignmentId: null,
      employeeId: 10,
      customerId: 20,
      customerSiteId: 30,
      customerName: '顧客A',
      siteName: '現場A',
      vehicleArrangementType: 'COMPANY',
      passengerCount: 0,
      workDescription: '',
      _isNew: true,
      _isUpdated: false,
      _isDeleted: false,
    } as DailyPreparationAssignmentTableRow

    resetAssignmentRowValues(row)

    expect(row._isNew).toBe(false)
    expect(row._isDeleted).toBe(false)
    expect(buildAssignmentSaveItems([row])).toEqual([])
  })
})
