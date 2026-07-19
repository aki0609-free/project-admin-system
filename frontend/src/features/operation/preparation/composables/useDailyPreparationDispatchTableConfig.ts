import { computed, type Ref } from 'vue'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { DailyPreparationDispatchResponse } from '../types/dailyPreparationApiTypes'
import type { DailyPreparationAssignmentTableRow } from './useDailyPreparationAssignmentTableConfig'

export type DailyPreparationDispatchTableRow = SimpleTableEditableRow & {
  id: number
  dispatchId: number | null

  customerId: number | null
  customerSiteId: number | null

  customerName: string
  siteName: string
  distanceFromCompanyKm: number

  vehicleCount: number
  note: string

  _isNew: boolean
  _isUpdated: boolean
  _isDeleted: boolean

  raw: DailyPreparationDispatchResponse | null
}

export const useDailyPreparationDispatchTableConfig = (
  rows: Ref<DailyPreparationDispatchTableRow[]>,
) => {
  const columns = computed<SimpleTableColumnDef<DailyPreparationDispatchTableRow>[]>(() => [
    {
      title: '顧客',
      key: 'customerName',
      width: '200px',
      editable: false,
      type: 'text',
      filter: { type: 'text' },
    },
    {
      title: '現場',
      key: 'siteName',
      width: '200px',
      editable: false,
      type: 'text',
      filter: { type: 'text' },
    },
    {
      title: '距離km',
      key: 'distanceFromCompanyKm',
      width: '200px',
      editable: false,
      type: 'number',
      filter: { type: 'text' },
    },
    {
      title: '配車台数',
      key: 'vehicleCount',
      width: '200px',
      editable: true,
      type: 'text',
      filter: { type: 'text' },
    },
    {
      title: '備考',
      key: 'note',
      width: '200px',
      editable: true,
      type: 'text',
      filter: { type: 'text' },
    },
  ])

  const filterRules = computed(() =>
    createSimpleTableFilterRules<DailyPreparationDispatchTableRow>(columns.value),
  )

  return {
    rows,
    columns,
    filterRules,
  }
}

export const createDispatchRowsFromAssignments = (
  assignmentRows: DailyPreparationAssignmentTableRow[],
  dispatches: DailyPreparationDispatchResponse[],
  currentRows: DailyPreparationDispatchTableRow[] = [],
  getDistanceFromSiteId: (siteId: number | null) => number = () => 0,
): DailyPreparationDispatchTableRow[] => {
  const dispatchMap = new Map<number, DailyPreparationDispatchResponse>()
  const currentMap = new Map<number, DailyPreparationDispatchTableRow>()
  const assignmentSiteMap = new Map<number, DailyPreparationAssignmentTableRow>()

  dispatches.forEach((dispatch) => {
    if (dispatch.customerSiteId != null) {
      dispatchMap.set(dispatch.customerSiteId, dispatch)
    }
  })

  currentRows.forEach((row) => {
    if (row.customerSiteId != null) {
      currentMap.set(row.customerSiteId, row)
    }
  })

  assignmentRows.forEach((assignment) => {
    if (assignment.customerId == null) return
    if (assignment.customerSiteId == null) return

    if (!assignmentSiteMap.has(assignment.customerSiteId)) {
      assignmentSiteMap.set(assignment.customerSiteId, assignment)
    }
  })

  return Array.from(assignmentSiteMap.values()).map((assignment) => {
    const siteId = assignment.customerSiteId
    const dispatch = siteId != null ? dispatchMap.get(siteId) ?? null : null
    const current = siteId != null ? currentMap.get(siteId) ?? null : null

    const distance =
      dispatch?.distanceFromCompanyKm ??
      current?.distanceFromCompanyKm ??
      getDistanceFromSiteId(siteId)

    return {
      id: dispatch?.id ?? current?.id ?? -Number(`${siteId}${assignment.employeeId}`),

      dispatchId: dispatch?.id ?? current?.dispatchId ?? null,

      customerId: assignment.customerId,
      customerSiteId: assignment.customerSiteId,

      customerName: assignment.customerName,
      siteName: assignment.siteName,
      distanceFromCompanyKm: distance ?? 0,

      vehicleCount: current?.vehicleCount ?? dispatch?.vehicleCount ?? 0,
      note: current?.note ?? dispatch?.note ?? '',

      _isNew: current?._isNew ?? false,
      _isUpdated: current?._isUpdated ?? false,
      _isDeleted: current?._isDeleted ?? false,

      raw: dispatch,
    }
  })
}