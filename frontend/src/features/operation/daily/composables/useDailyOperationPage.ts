import { computed, ref, watch } from 'vue'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import { useDailyPaymentsQuery } from '../api/useDailyPaymentsQuery'
import type { DailyPaymentBulkSaveItemRequest } from '../types/dailyPaymentApiTypes'
import {
  createDailyPaymentRows,
  type DailyPaymentTableRow,
} from './useDailyPaymentTableConfig'
import { useBulkSaveDailyPaymentsMutation } from '../api/useBulkSaveDailyPaymentMutation'

const today = () => new Date().toISOString().slice(0, 10)

export const useDailyOperationPage = () => {
  const paymentDate = ref(today())
  const activeTab = ref<'summary' | 'details' | 'reports'>('summary')
  const rows = ref<DailyPaymentTableRow[]>([])

  const paymentsQuery = useDailyPaymentsQuery(paymentDate)
  const bulkSaveMutation = useBulkSaveDailyPaymentsMutation()

  watch(
    () => paymentsQuery.payments.value,
    (payments) => {
      rows.value = createDailyPaymentRows(payments)
    },
    { immediate: true },
  )

  const markUpdated = (row: DailyPaymentTableRow) => {
    if (row.paymentId == null) {
      row._isNew = true
      row._isUpdated = false
      row._isDeleted = false
      return
    }

    row._isNew = false
    row._isUpdated = true
    row._isDeleted = false
  }

  const handleCellUpdate = ({
    id,
    field,
    value,
  }: {
    id: number
    field: keyof DailyPaymentTableRow
    value: unknown
  }) => {
    const row = rows.value.find((item) => item.id === id)
    if (!row) return

    if (field === 'actualAmount') {
      row.actualAmount = Number(value ?? 0)
      markUpdated(row)
      return
    }

    if (field === 'note') {
      row.note = String(value ?? '')
      markUpdated(row)
    }
  }

  const save = async () => {
    const items: DailyPaymentBulkSaveItemRequest[] = rows.value
      .filter((row) => row._isNew || row._isUpdated || row._isDeleted)
      .map((row) => ({
        id: row.paymentId,
        employeeId: row.employeeId,
        plannedAmount: row.plannedAmount,
        actualAmount: row.actualAmount,
        status: 'PENDING',
        note: row.note.trim() || null,
        isNew: row._isNew,
        isUpdated: row._isUpdated,
        isDeleted: row._isDeleted,
      }))

    if (items.length === 0) return

    await bulkSaveMutation.mutateAsync({
      paymentDate: paymentDate.value,
      items,
    })

    await paymentsQuery.refetch()
  }

  const hasDirtyRows = computed(() =>
    rows.value.some((row) => row._isNew || row._isUpdated || row._isDeleted),
  )

  const employeeCount = computed(() => rows.value.length)

  const totalPlannedAmount = computed(() =>
    rows.value.reduce((sum, row) => sum + Number(row.plannedAmount ?? 0), 0),
  )

  const totalActualAmount = computed(() =>
    rows.value.reduce((sum, row) => sum + Number(row.actualAmount ?? 0), 0),
  )

  const tabs = [
    { label: '概要', value: 'summary' },
    { label: '明細', value: 'details' },
    { label: '帳票', value: 'reports' },
  ]

  const leftToolbarItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: '保存',
      color: 'primary',
      disabled: !hasDirtyRows.value,
      onClick: save,
    },
  ])

  const rightToolbarItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: '再読込',
      color: 'secondary',
      onClick: async () => {
        await paymentsQuery.refetch()
      },
    },
  ])

  const countText = computed(() => `対象者：${rows.value.length} 人`)

  return {
    paymentDate,
    activeTab,
    tabs,

    rows,
    countText,

    employeeCount,
    totalPlannedAmount,
    totalActualAmount,

    paymentsQuery,

    leftToolbarItems,
    rightToolbarItems,

    handleCellUpdate,
  }
}