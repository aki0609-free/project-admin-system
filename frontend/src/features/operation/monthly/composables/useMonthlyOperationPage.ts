import { computed, ref } from 'vue'

import type { ToolbarItem } from '@/shared/components/toolbar/types/types'

import { useClosingSummaryQuery } from '../api/useClosingSummaryQuery'
import { useCloseClosingMutation } from '../api/useCloseClosingMutation'
import { useRecloseClosingMutation } from '../api/useRecloseClosingMutation'

const currentMonth = () => new Date().toISOString().slice(0, 7)

export const useMonthlyOperationPage = () => {
  const targetMonth = ref(currentMonth())

  const activeTab = ref<'summary' | 'reports'>('summary')

  const summaryQuery = useClosingSummaryQuery(targetMonth)

  const closeMutation = useCloseClosingMutation()
  const recloseMutation = useRecloseClosingMutation()

  const summary = computed(() => summaryQuery.summary.value)

  const isClosed = computed(
    () => summary.value?.closing?.status === 'CLOSED',
  )

  const tabs = [
    { label: '概要', value: 'summary' },
    { label: '帳票', value: 'reports' },
  ]

  const closeClosing = async () => {
    if (!confirm(`${targetMonth.value} を締め処理しますか？`)) {
      return
    }

    await closeMutation.mutateAsync(targetMonth.value)

    await summaryQuery.refetch()
  }

  const recloseClosing = async () => {
    if (
      !confirm(
        `${targetMonth.value} を再締めしますか？\nVersionが1つ増えます。`,
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
      label: isClosed.value ? '再締め' : '締め処理',
      color: 'primary',
      onClick: isClosed.value
        ? recloseClosing
        : closeClosing,
    },
  ])

  const rightToolbarItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: '再読込',
      color: 'secondary',
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