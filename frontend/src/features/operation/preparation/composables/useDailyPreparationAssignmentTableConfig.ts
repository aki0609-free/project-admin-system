import { computed, type Ref } from 'vue'
import { storeToRefs } from 'pinia'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { EmployeeListItemResponse } from '@/features/employees/types/employeeApiTypes'
import type { DailyPreparationAssignmentResponse } from '../types/dailyPreparationApiTypes'
import type { VehicleArrangementType } from '../types/dailyPreparationApiTypes'
import { useCustomerMasterStore } from '@/features/customer/store/useCustomerMasterStore'

export type DailyPreparationAssignmentTableRow = SimpleTableEditableRow & {
  id: number

  assignmentId: number | null
  employeeId: number
  employeeCode: string
  employeeName: string

  customerId: number | null
  customerSiteId: number | null
  customerName: string
  siteName: string

  vehicleArrangementType: VehicleArrangementType
  passengerCount: number

  workDescription: string

  _isNew: boolean
  _isUpdated: boolean
  _isDeleted: boolean

  raw: DailyPreparationAssignmentResponse | null
}

export const useDailyPreparationAssignmentTableConfig = (
  rows: Ref<DailyPreparationAssignmentTableRow[]>,
) => {
  const customerStore = useCustomerMasterStore()
  const { customerOptions } = storeToRefs(customerStore)

  const columns = computed<SimpleTableColumnDef<DailyPreparationAssignmentTableRow>[]>(() => [
    {
      title: '社員コード',
      key: 'employeeCode',
      width: '200px',
      filter: { type: 'text' },
      editable: false,
    },
    {
      title: '氏名',
      key: 'employeeName',
      width: '200px',
      filter: { type: 'text' },
      editable: false,
    },
    {
      title: '顧客',
      key: 'customerId',
      width: '200px',
      editable: true,
      filter: { type: 'text' },
      type: 'select',
      enumOptions: customerOptions.value,
    },
    {
      title: '現場',
      key: 'customerSiteId',
      width: '200px',
      editable: true,
      filter: { type: 'text' },
      type: 'select',
      enumOptions: (row) => customerStore.siteOptions(row.customerId),
    },
    {
      title: '車両手配',
      key: 'vehicleArrangementType',
      width: '240px',
      editable: true,
      type: 'select',
      enumOptions: [
        { title: '車両なし', value: 'NONE' },
        { title: '会社手配（顧客請求）', value: 'COMPANY' },
        { title: '社員手配・運転者（顧客請求＋運転手当）', value: 'EMPLOYEE' },
        { title: '同乗者（請求・運転手当なし）', value: 'PASSENGER' },
      ],
      filter: { type: 'select' },
    },
    {
      title: '同乗者数',
      key: 'passengerCount',
      width: '160px',
      editable: true,
      type: 'number',
      min: 0,
      step: 1,
      filter: { type: 'number' },
    },
    {
      title: '作業内容',
      key: 'workDescription',
      width: '200px',
      editable: true,
      filter: { type: 'text' },
      type: 'text',
    },
  ])

  const filterRules = computed(() =>
    createSimpleTableFilterRules<DailyPreparationAssignmentTableRow>(columns.value),
  )

  return {
    rows,
    columns,
    filterRules,
  }
}

export const createAssignmentRows = (
  employees: EmployeeListItemResponse[],
  assignments: DailyPreparationAssignmentResponse[],
): DailyPreparationAssignmentTableRow[] => {
  const map = new Map<number, DailyPreparationAssignmentResponse>()

  assignments.forEach((assignment) => {
    map.set(assignment.employeeId, assignment)
  })

  return employees.map((employee) => {
    const assignment = map.get(employee.id) ?? null

    return {
      id: employee.id,

      assignmentId: assignment?.id ?? null,
      employeeId: employee.id,
      employeeCode: employee.employeeCode,
      employeeName: employee.employeeName,

      customerId: assignment?.customerId ?? null,
      customerSiteId: assignment?.customerSiteId ?? null,
      customerName: assignment?.customerName ?? '',
      siteName: assignment?.siteName ?? '',

      vehicleArrangementType: assignment?.vehicleArrangementType ?? 'NONE',
      passengerCount: assignment?.passengerCount ?? 0,

      workDescription: assignment?.workDescription ?? '',

      _isNew: false,
      _isUpdated: false,
      _isDeleted: false,

      raw: assignment,
    }
  })
}
