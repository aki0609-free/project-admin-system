import { computed, reactive, ref, watch } from 'vue'

import type { NoticeResponse } from '@/features/dashboard/types/dashboardTypes'
import type { SearchPanelFieldDef } from '@/shared/components/search/types/searchPanelTypes'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'

export type NoticeBoardFilter = {
  keyword: string
  type: string
  from: string
  to: string
}

const colorMap: Record<string, string> = {
  important: 'red',
  warning: 'orange',
  info: 'blue',
}

const labelMap: Record<string, string> = {
  important: '重要',
  warning: '警告',
  info: '情報',
}

export const useNoticeBoard = (noticesGetter: () => NoticeResponse[], emitCreate: () => void) => {
  const page = ref(1)
  const itemsPerPage = ref(5)

  const detailDialog = ref(false)
  const deleteConfirmDialog = ref(false)
  const selectedNotice = ref<NoticeResponse | null>(null)

  const filter = reactive<NoticeBoardFilter>({
    keyword: '',
    type: 'all',
    from: '',
    to: '',
  })

  const searchFields: SearchPanelFieldDef<NoticeBoardFilter>[] = [
    {
      key: 'keyword',
      label: 'キーワード',
      placeholder: 'タイトル・内容で検索',
      type: 'text',
      md: 3,
      prependIcon: 'mdi-magnify',
    },

    {
      key: 'type',
      label: '種別',
      type: 'select',
      md: 3,
      prependIcon: 'mdi-tag-outline',
      options: [
        { title: 'すべて', value: 'all' },
        { title: '重要', value: 'important' },
        { title: '警告', value: 'warning' },
        { title: '情報', value: 'info' },
      ],
    },

    {
      key: 'from',
      label: '開始日',
      type: 'date',
      md: 3,
      prependIcon: 'mdi-calendar-start',
    },

    {
      key: 'to',
      label: '終了日',
      type: 'date',
      md: 3,
      prependIcon: 'mdi-calendar-end',
    },
  ]

  const leftToolbarItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: '作成',
      color: 'primary',
      onClick: emitCreate,
    },
  ])

  const rightToolbarItems = computed<ToolbarItem[]>(() => [])

  const filteredNotices = computed(() =>
    noticesGetter().filter((notice) => {
      const noticeType = notice.type?.toLowerCase() ?? 'info'

      if (filter.from && notice.end < filter.from) return false
      if (filter.to && notice.start > filter.to) return false
      if (filter.type !== 'all' && noticeType !== filter.type) return false

      if (filter.keyword) {
        const text = `${notice.title} ${notice.content ?? ''}`.toLowerCase()
        if (!text.includes(filter.keyword.toLowerCase())) return false
      }

      return true
    }),
  )

  const pagedNotices = computed(() => {
    const start = (page.value - 1) * itemsPerPage.value
    return filteredNotices.value.slice(start, start + itemsPerPage.value)
  })

  const pageCount = computed(() =>
    Math.max(Math.ceil(filteredNotices.value.length / itemsPerPage.value), 1),
  )

  const importantCount = computed(
    () => noticesGetter().filter((n) => n.type?.toLowerCase() === 'important').length,
  )

  const warningCount = computed(
    () => noticesGetter().filter((n) => n.type?.toLowerCase() === 'warning').length,
  )

  const pinnedCount = computed(() => noticesGetter().filter((n) => n.pinnedFlag).length)

  watch(
    filter,
    () => {
      page.value = 1
    },
    { deep: true },
  )

  const clearFilter = () => {
    filter.keyword = ''
    filter.type = 'all'
    filter.from = ''
    filter.to = ''
    page.value = 1
  }

  const getType = (notice: NoticeResponse) => notice.type?.toLowerCase() ?? 'info'

  const getColor = (notice: NoticeResponse) => {
    const type = getType(notice)
    return notice.color || colorMap[type] || 'blue'
  }

  const getLabel = (notice: NoticeResponse) => labelMap[getType(notice)] || '情報'

  const formatPeriod = (notice: NoticeResponse) => {
    if (notice.start === notice.end) return notice.start
    return `${notice.start} ～ ${notice.end}`
  }

  const openDetail = (notice: NoticeResponse) => {
    selectedNotice.value = notice
    detailDialog.value = true
  }

  return {
    filter,
    searchFields,
    leftToolbarItems,
    rightToolbarItems,

    page,
    pageCount,
    detailDialog,
    deleteConfirmDialog,
    selectedNotice,

    pagedNotices,
    importantCount,
    warningCount,
    pinnedCount,

    clearFilter,
    getColor,
    getLabel,
    formatPeriod,
    openDetail,
  }
}
