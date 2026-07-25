<script setup lang="ts">
import { computed, ref } from 'vue'

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
import { useAuth } from '@/shared/auth/composables/useAuth'
import { Role } from '@/shared/auth/types/types'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'

const tabs = [
  { label: '概要', value: 'summary' },
  { label: 'カレンダー', value: 'calendar' },
]

const activeTab = ref('summary')

const formatLocalDate = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const calendarDate = ref(formatLocalDate(new Date()))

const calendarFrom = computed(() => {
  const date = new Date(`${calendarDate.value}T00:00:00`)
  return formatLocalDate(new Date(date.getFullYear(), date.getMonth(), 1))
})

const calendarTo = computed(() => {
  const date = new Date(`${calendarDate.value}T00:00:00`)
  return formatLocalDate(new Date(date.getFullYear(), date.getMonth() + 1, 0))
})

const noticeDialog = ref(false)
const editingNotice = ref<NoticeResponse | null>(null)

const { notices, refetch: refetchNotices } = useNoticesQuery()
const { notices: calendarNotices, refetch: refetchCalendarNotices } =
  useNoticeCalendarQuery(calendarFrom, calendarTo)

const { hasRole } = useAuth()
const isSysAdmin = computed(() => hasRole(Role.SYS_ADMIN))
const canManageNotices = computed(
  () => isSysAdmin.value || hasRole(Role.ADMIN),
)

const canEditNotice = (notice: NoticeResponse) =>
  canManageNotices.value && notice.sourceType === 'MANUAL'

const canDeleteNotice = (notice: NoticeResponse) =>
  canManageNotices.value &&
  (notice.sourceType === 'MANUAL' || isSysAdmin.value)

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
  if (!canEditNotice(notice)) return

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
  if (!canDeleteNotice(notice)) return

  await deleteNoticeMutation.mutateAsync(notice.id)

  await refetchAll()
}
</script>

<template>
  <TabLayout v-model="activeTab" :tabs="tabs">
    <template #default="{ active }">
      <NoticeBoard
        v-if="active === 'summary'"
        :notices="notices"
        :can-create="canManageNotices"
        :can-edit="canEditNotice"
        :can-delete="canDeleteNotice"
        @create="openCreateDialog"
        @edit="openEditDialog"
        @delete="deleteNotice"
      />

      <NoticeCalendarView
        v-else-if="active === 'calendar'"
        v-model:calendar-date="calendarDate"
        :notices="calendarNotices"
        :can-edit="canEditNotice"
        :can-delete="canDeleteNotice"
        @edit="openEditDialog"
        @delete="deleteNotice"
      />
    </template>
  </TabLayout>

  <NoticeCreateDialog
    v-if="canManageNotices"
    v-model="noticeDialog"
    :notice="editingNotice"
    @submit="saveNotice"
  />
</template>
