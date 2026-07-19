<script setup lang="ts">
import { ref } from 'vue'

import NoticeBoard from '../components/NoticeBoard.vue'
import NoticeCreateDialog from '../components/NoticeCreateDialog.vue'
import NoticeCalendarView from '../components/NoticeCalendarView.vue'
import { useCreateNoticeMutation } from '../api/useCreateNoticeMutation'
import { useDeleteNoticeMutation } from '../api/useDeleteNoticeMutation'
import { useNoticeCalendarQuery } from '../api/useNoticeCalendarQuery'
import { useNoticesQuery } from '../api/useNoticesQuery'
import { useUpdateNoticeMutation } from '../api/useUpdateNoticeMutation'

import type {
  NoticeCreateRequest,
  NoticeResponse,
} from '@/features/dashboard/types/dashboardTypes'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'

const tabs = [
  { label: '概要', value: 'summary' },
  { label: 'カレンダー', value: 'calendar' },
]

const activeTab = ref('summary')
const selectedNotice = ref<NoticeResponse | null>(null)

const from = ref('2026-05-01')
const to = ref('2026-05-31')

const noticeDialog = ref(false)
const editingNotice = ref<NoticeResponse | null>(null)

const { notices, refetch: refetchNotices } = useNoticesQuery()
const { notices: calendarNotices, refetch: refetchCalendarNotices } =
  useNoticeCalendarQuery(from, to)

const createNoticeMutation = useCreateNoticeMutation()
const updateNoticeMutation = useUpdateNoticeMutation()
const deleteNoticeMutation = useDeleteNoticeMutation()

const refetchAll = async () => {
  await refetchNotices()
  await refetchCalendarNotices()
}

const openCreateDialog = () => {
  editingNotice.value = null
  noticeDialog.value = true
}

const openEditDialog = (notice: NoticeResponse) => {
  editingNotice.value = notice
  noticeDialog.value = true
}

const saveNotice = async (request: NoticeCreateRequest) => {
  if (editingNotice.value?.id) {
    await updateNoticeMutation.mutateAsync({
      id: editingNotice.value.id,
      body: request,
    })
  } else {
    await createNoticeMutation.mutateAsync(request)
  }

  await refetchAll()

  noticeDialog.value = false
  editingNotice.value = null
}

const deleteNotice = async (notice: NoticeResponse) => {
  await deleteNoticeMutation.mutateAsync(notice.id)

  await refetchAll()

  if (selectedNotice.value?.id === notice.id) {
    selectedNotice.value = null
  }
}
</script>

<template>
  <TabLayout v-model="activeTab" :tabs="tabs">
    <template #default="{ active }">
      <NoticeBoard
        v-if="active === 'summary'"
        :notices="notices"
        @create="openCreateDialog"
        @edit="openEditDialog"
        @delete="deleteNotice"
      />

      <NoticeCalendarView
        v-else-if="active === 'calendar'"
        :notices="calendarNotices"
        :selected-notice="selectedNotice"
      />
    </template>
  </TabLayout>

  <NoticeCreateDialog
    v-model="noticeDialog"
    :notice="editingNotice"
    @submit="saveNotice"
  />
</template>