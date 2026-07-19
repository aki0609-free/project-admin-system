/* eslint-disable @typescript-eslint/no-non-null-assertion */
import { computed, ref, watch, type Ref } from 'vue'

import type { NoticeResponse } from '../types/dashboardTypes'

type CalendarTimestamp = {
  date: string
}

type CalendarNoticeEvent = Omit<NoticeResponse, 'start' | 'end'> & {
  start: Date
  end: Date
}

export const useNoticeCalendar = (
  notices: Ref<NoticeResponse[]>,
  selectedNotice: Ref<NoticeResponse | null>,
) => {
  const calendarDate = ref(new Date().toISOString().slice(0, 10))

  const dayDialog = ref(false)

  const selectedDate = ref<string | null>(null)

  const selectedNoticeDetail = ref<NoticeResponse | null>(null)

  const detailDialog = ref(false)

  const calendarEvents = computed<CalendarNoticeEvent[]>(() =>
    notices.value.map((notice) => ({
      ...notice,
      start: new Date(`${notice.start}T00:00:00`),
      end: new Date(`${notice.end}T23:59:59`),
    })),
  )

  const selectedNotices = computed(() => {
    if (!selectedDate.value) {
      return []
    }

    return notices.value.filter(
      (notice) => notice.start <= selectedDate.value! && notice.end >= selectedDate.value!,
    )
  })

  const titleText = computed(() => {
    const date = new Date(calendarDate.value)

    return `${date.getFullYear()}年 ${date.getMonth() + 1}月`
  })

  watch(selectedNotice, (notice) => {
    if (!notice) {
      return
    }

    calendarDate.value = notice.start

    openDayDialog(notice.start)
  })

  const formatDate = (date: Date) => {
    const yyyy = date.getFullYear()
    const mm = String(date.getMonth() + 1).padStart(2, '0')
    const dd = String(date.getDate()).padStart(2, '0')

    return `${yyyy}-${mm}-${dd}`
  }

  const formatPeriod = (notice: NoticeResponse) => {
    if (notice.start === notice.end) {
      return notice.start
    }

    return `${notice.start} ～ ${notice.end}`
  }

  const sourceLabel = (notice: NoticeResponse) => (notice.sourceType === 'AUTO' ? '自動' : '手動')

  const moveMonth = (amount: number) => {
    const date = new Date(calendarDate.value)

    date.setMonth(date.getMonth() + amount)

    calendarDate.value = formatDate(date)
  }

  const movePrevious = () => moveMonth(-1)

  const moveNext = () => moveMonth(1)

  const moveToday = () => (calendarDate.value = formatDate(new Date()))

  const openDayDialog = (date: string) => {
    selectedDate.value = date
    dayDialog.value = true
  }

  const openNoticeDetail = (notice: NoticeResponse) => {
    selectedNoticeDetail.value = notice
    detailDialog.value = true
  }

  const closeDayDialog = () => (dayDialog.value = false)

  const closeDetailDialog = () => (detailDialog.value = false)

  const onClickDate = (_event: Event, day: CalendarTimestamp) => {
    openDayDialog(day.date)
  }

  const getType = (notice: NoticeResponse) => notice.type?.toLowerCase() ?? 'info'

  const getColor = (notice: NoticeResponse) => {
    if (notice.color) return notice.color

    const type = getType(notice)

    if (type === 'important') return 'red'
    if (type === 'warning') return 'orange'

    return 'blue'
  }

  const getLabel = (notice: NoticeResponse) => {
    const type = getType(notice)

    if (type === 'important') return '重要'
    if (type === 'warning') return '警告'

    return '情報'
  }

  return {
    calendarDate,

    dayDialog,
    detailDialog,

    selectedDate,
    selectedNotices,
    selectedNoticeDetail,

    calendarEvents,
    titleText,

    movePrevious,
    moveNext,
    moveToday,

    openDayDialog,
    openNoticeDetail,

    closeDayDialog,
    closeDetailDialog,

    onClickDate,

    getColor,
    getLabel,

    formatDate,
    formatPeriod,
    sourceLabel,
  }
}
