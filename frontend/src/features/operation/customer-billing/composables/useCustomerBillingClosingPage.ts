/* eslint-disable @typescript-eslint/no-invalid-void-type */
import { computed, onMounted, ref, watch } from 'vue'

import { get, post } from '@/shared/api/http'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'

import type {
  CustomerBillingBulkClosing,
  CustomerBillingClosing,
  CustomerBillingSummary,
  CustomerBillingTarget,
} from '../types/customerBillingTypes'

const currentMonth = () => new Date().toISOString().slice(0, 7)

export const useCustomerBillingClosingPage = () => {
  const targetMonth = ref(currentMonth())
  const activeTab = ref<'customers' | 'reports'>('customers')
  const summary = ref<CustomerBillingSummary | null>(null)
  const loading = ref(false)
  const processingCustomerIds = ref<number[]>([])

  const tabs = [
    { label: '顧客一覧', value: 'customers' },
    { label: '帳票一覧', value: 'reports' },
  ]

  const load = async () => {
    if (!targetMonth.value) return
    loading.value = true
    try {
      summary.value = await get<CustomerBillingSummary>(
        '/api/operation/customer-billing/summary',
        { params: { query: { targetMonth: targetMonth.value } } },
      )
    } finally {
      loading.value = false
    }
  }

  const executeAll = async () => {
    if (!confirm(
      `${targetMonth.value} の締日到来済み・未締め顧客を一括で締めますか？\n締日前と締め済みの顧客は除外されます。`,
    )) return

    loading.value = true
    try {
      const result = await post<CustomerBillingBulkClosing, void>(
        '/api/operation/customer-billing/close-all',
        undefined,
        { params: { query: { targetMonth: targetMonth.value } } },
      )
      await load()
      const message = [
        `完了: ${result.completedCount}社`,
        `締日前のため除外: ${result.skippedBeforeClosingDateCount}社`,
        `締め済みのため除外: ${result.alreadyClosedCount}社`,
        `失敗: ${result.failedCount}社`,
        ...result.errors,
      ].join('\n')
      window.alert(message)
    } finally {
      loading.value = false
    }
  }

  const executeCustomer = async (customer: CustomerBillingTarget) => {
    const reclose = customer.closing?.status === 'CLOSED'
    const earlyWarning = !customer.closingDateReached
      ? '\nこの顧客はまだ締日前です。例外的に実行しますか？'
      : ''
    const versionWarning = reclose ? '\nVersionが1つ増えます。' : ''
    if (!confirm(
      `${customer.customerName}を${reclose ? '再締め' : '締め'}しますか？${earlyWarning}${versionWarning}`,
    )) return

    processingCustomerIds.value.push(customer.customerId)
    try {
      await post<CustomerBillingClosing, void>(
        reclose
          ? '/api/operation/customer-billing/reclose'
          : '/api/operation/customer-billing/close',
        undefined,
        {
          params: {
            query: {
              targetMonth: targetMonth.value,
              customerId: customer.customerId,
            },
          },
        },
      )
      await load()
    } finally {
      processingCustomerIds.value = processingCustomerIds.value.filter(
        (customerId) => customerId !== customer.customerId,
      )
    }
  }

  const isCustomerLoading = (customerId: number) =>
    processingCustomerIds.value.includes(customerId)

  const leftToolbarItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: '全顧客締め',
      color: 'primary',
      disabled: loading.value || !summary.value?.customers.length,
      onClick: executeAll,
    },
  ])

  const rightToolbarItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: '再読込',
      color: 'secondary',
      disabled: loading.value,
      onClick: load,
    },
  ])

  watch(targetMonth, load)
  onMounted(load)

  return {
    targetMonth,
    activeTab,
    tabs,
    summary,
    loading,
    leftToolbarItems,
    rightToolbarItems,
    executeCustomer,
    isCustomerLoading,
  }
}
