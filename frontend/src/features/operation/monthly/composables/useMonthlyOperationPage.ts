import { computed, ref } from 'vue'

import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import {
  businessMonthWithOffset,
  formatYearMonth,
} from '@/shared/utils/DateUtils'

import { useClosingSummaryQuery } from '../api/useClosingSummaryQuery'
import { useCloseClosingMutation } from '../api/useCloseClosingMutation'
import { useRecloseClosingMutation } from '../api/useRecloseClosingMutation'

export const useMonthlyOperationPage = () => {
  const targetMonth = ref(businessMonthWithOffset(-1))

  const activeTab = ref<'summary' | 'reports'>('summary')

  const summaryQuery = useClosingSummaryQuery(targetMonth)

  const closeMutation = useCloseClosingMutation()
  const recloseMutation = useRecloseClosingMutation()

  const summary = computed(() => summaryQuery.summary.value)

  const isClosed = computed(
    () => summary.value?.closing?.status === 'CLOSED',
  )
  const isProcessing = computed(
    () => summary.value?.closing?.status === 'PROCESSING',
  )
  const isFailed = computed(
    () => summary.value?.closing?.status === 'FAILED',
  )
  const hasCompletedVersion = computed(
    () => (summary.value?.closing?.closingVersion ?? 0) > 0,
  )
  const shouldReclose = computed(
    () => isClosed.value || (isFailed.value && hasCompletedVersion.value),
  )
  const busy = computed(
    () => closeMutation.isPending.value
      || recloseMutation.isPending.value
      || isProcessing.value,
  )

  const tabs = [
    { label: '概要', value: 'summary' },
    { label: '帳票', value: 'reports' },
  ]

  const closeClosing = async () => {
    if (!confirm(`${formatYearMonth(targetMonth.value)}を締め処理しますか？`)) {
      return
    }

    await closeMutation.mutateAsync(targetMonth.value)

    await summaryQuery.refetch()
  }

  const recloseClosing = async () => {
    if (
      !confirm(
        `${formatYearMonth(targetMonth.value)}を再締めしますか？\nVersionが1つ増えます。`,
      )
    ) {
      return
    }

    await recloseMutation.mutateAsync(targetMonth.value)

    await summaryQuery.refetch()
  }

  const leftToolbarItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: shouldReclose.value ? '再締め' : '締め処理',
      color: 'primary',
      disabled: busy.value,
      loading: busy.value,
      onClick: shouldReclose.value
        ? recloseClosing
        : closeClosing,
    },
  ])

  const rightToolbarItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: '再読込',
      color: 'secondary',
      disabled: busy.value,
      onClick: async () => {
        await summaryQuery.refetch()
      },
    },
  ])

  return {
    targetMonth,

    activeTab,
    tabs,

    summary,

    leftToolbarItems,
    rightToolbarItems,
  }
}
