import { computed, ref, watch } from 'vue'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import { useDailyPaymentsQuery } from '../api/useDailyPaymentsQuery'
import {
  createDailyPaymentRows,
  type DailyPaymentTableRow,
} from './useDailyPaymentTableConfig'
import { businessDateWithOffset } from '@/shared/utils/DateUtils'

export const useDailyOperationPage = () => {
  const paymentDate = ref(businessDateWithOffset(-1))
  const activeTab = ref<'summary' | 'details' | 'reports'>('summary')
  const rows = ref<DailyPaymentTableRow[]>([])

  const paymentsQuery = useDailyPaymentsQuery(paymentDate)

  watch(
    () => paymentsQuery.payments.value,
    (payments) => {
      rows.value = createDailyPaymentRows(payments)
    },
    { immediate: true },
  )

  const employeeCount = computed(() => rows.value.length)

  const totalPaymentAmount = computed(() =>
    rows.value.reduce((sum, row) => sum + Number(row.plannedAmount ?? 0), 0),
  )

  const tabs = [
    { label: '概要', value: 'summary' },
    { label: '明細', value: 'details' },
    { label: '帳票', value: 'reports' },
  ]

  const leftToolbarItems = computed<ToolbarItem[]>(() => [])

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
    totalPaymentAmount,

    paymentsQuery,

    leftToolbarItems,
    rightToolbarItems,

  }
}
