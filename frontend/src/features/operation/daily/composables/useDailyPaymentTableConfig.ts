import { computed, type Ref } from 'vue'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type {
  DailyPaymentResponse,
  DailyPaymentStatus,
} from '../types/dailyPaymentApiTypes'

export type DailyPaymentTableRow = SimpleTableEditableRow & {
  id: number
  paymentId: number | null

  employeeId: number
  employeeCode: string
  employeeName: string

  plannedAmount: number
  actualAmount: number

  status: DailyPaymentStatus
  paidAt: string
  note: string

  _isNew: boolean
  _isUpdated: boolean
  _isDeleted: boolean

  raw: DailyPaymentResponse
}

export const createDailyPaymentRows = (
  payments: DailyPaymentResponse[],
): DailyPaymentTableRow[] =>
  payments.map((payment) => ({
    id: payment.id ?? -payment.employeeId,
    paymentId: payment.id,

    employeeId: payment.employeeId,
    employeeCode: payment.employeeCode,
    employeeName: payment.employeeName,

    plannedAmount: Number(payment.plannedAmount ?? 0),
    actualAmount: Number(payment.actualAmount ?? 0),

    status: payment.status ?? 'PENDING',
    paidAt: payment.paidAt ?? '',
    note: payment.note ?? '',

    _isNew: payment.id == null,
    _isUpdated: false,
    _isDeleted: false,

    raw: payment,
  }))

export const useDailyPaymentTableConfig = (
  rows: Ref<DailyPaymentTableRow[]>,
) => {
  const columns = computed<SimpleTableColumnDef<DailyPaymentTableRow>[]>(() => [
    {
      title: '社員コード',
      key: 'employeeCode',
      width: '180px',
      editable: false,
      filter: { type: 'text' },
    },
    {
      title: '氏名',
      key: 'employeeName',
      width: '180px',
      editable: false,
      filter: { type: 'text' },
    },
    {
      title: '予定日払い額',
      key: 'plannedAmount',
      width: '180px',
      type: 'number',
      editable: false,
      filter: { type: 'text' },
    },
    {
      title: '実支払額',
      key: 'actualAmount',
      width: '180px',
      type: 'number',
      editable: true,
      filter: { type: 'text' },
    },
    {
      title: '備考',
      key: 'note',
      width: '180px',
      type: 'text',
      editable: true,
      filter: { type: 'text' },
    },
  ])

  const filterRules = computed(() =>
    createSimpleTableFilterRules<DailyPaymentTableRow>(columns.value),
  )

  return {
    rows,
    columns,
    filterRules,
  }
}