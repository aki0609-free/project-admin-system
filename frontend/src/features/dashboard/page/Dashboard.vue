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
import {
  formatLocalDate,
  getCalendarMonthRange,
} from '../utils/dashboardDate'

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

const calendarDate = ref(formatLocalDate(new Date()))

const calendarFrom = computed(() => getCalendarMonthRange(calendarDate.value).from)
const calendarTo = computed(() => getCalendarMonthRange(calendarDate.value).to)

const noticeDialog = ref(false)
const editingNotice = ref<NoticeResponse | null>(null)

const noticesQuery = useNoticesQuery()
const calendarQuery = useNoticeCalendarQuery(calendarFrom, calendarTo)
const notices = noticesQuery.notices
const calendarNotices = calendarQuery.notices
const operationError = ref('')

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
const saving = computed(
  () => createNoticeMutation.isPending.value || updateNoticeMutation.isPending.value,
)
const deleting = computed(() => deleteNoticeMutation.isPending.value)

const refetchAll = async () => {
  await Promise.all([noticesQuery.refetch(), calendarQuery.refetch()])
}

const toErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof Error && error.message) return error.message
  if (typeof error === 'object' && error !== null) {
    const candidate = error as Record<string, unknown>
    if (typeof candidate.message === 'string') return candidate.message
  }
  return fallback
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
  if (saving.value) return
  operationError.value = ''

  try {
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
  } catch (error) {
    operationError.value = toErrorMessage(error, 'お知らせの保存に失敗しました。')
  }
}

const deleteNotice = async (notice: NoticeResponse) => {
  if (!canDeleteNotice(notice) || deleting.value) return
  operationError.value = ''

  try {
    await deleteNoticeMutation.mutateAsync(notice.id)

    await refetchAll()
  } catch (error) {
    operationError.value = toErrorMessage(error, 'お知らせの削除に失敗しました。')
  }
}
</script>

<template>
  <TabLayout v-model="activeTab" :tabs="tabs">
    <template #default="{ active }">
      <NoticeBoard
        v-if="active === 'summary'"
        :notices="notices"
        :loading="noticesQuery.isPending.value"
        :error="noticesQuery.isError.value"
        :deleting="deleting"
        :can-create="canManageNotices"
        :can-edit="canEditNotice"
        :can-delete="canDeleteNotice"
        @create="openCreateDialog"
        @edit="openEditDialog"
        @delete="deleteNotice"
        @retry="noticesQuery.refetch()"
      />

      <NoticeCalendarView
        v-else-if="active === 'calendar'"
        v-model:calendar-date="calendarDate"
        :notices="calendarNotices"
        :loading="calendarQuery.isPending.value || calendarQuery.isFetching.value"
        :error="calendarQuery.isError.value"
        :deleting="deleting"
        :can-edit="canEditNotice"
        :can-delete="canDeleteNotice"
        @edit="openEditDialog"
        @delete="deleteNotice"
        @retry="calendarQuery.refetch()"
      />
    </template>
  </TabLayout>

  <NoticeCreateDialog
    v-if="canManageNotices"
    v-model="noticeDialog"
    :notice="editingNotice"
    :saving="saving"
    @submit="saveNotice"
  />

  <v-snackbar
    :model-value="!!operationError"
    color="error"
    timeout="6000"
    @update:model-value="value => { if (!value) operationError = '' }"
  >
    {{ operationError }}
  </v-snackbar>
</template>
