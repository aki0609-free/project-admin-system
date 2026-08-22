<script setup lang="ts">
import type { NoticeResponse } from '@/features/dashboard/types/dashboardTypes'
import { useNoticeBoard } from '@/features/dashboard/composables/useNoticeBoard'
import NoticeBoardDetailDialog from '@/features/dashboard/components/NoticeBoardDetailDialog.vue'
import NoticeBoardList from '@/features/dashboard/components/NoticeBoardList.vue'
import NoticeBoardSummary from '@/features/dashboard/components/NoticeBoardSummary.vue'
import SearchPanel from '@/shared/components/search/SearchPanel.vue'
import BatchPageToolbar from '@/shared/ui/toolbar/BatchPageToolbar.vue'

const props = defineProps<{
  notices: NoticeResponse[]
  loading: boolean
  error: boolean
  deleting: boolean
  canCreate: boolean
  canEdit: (notice: NoticeResponse) => boolean
  canDelete: (notice: NoticeResponse) => boolean
}>()

const emit = defineEmits<{
  create: []
  edit: [notice: NoticeResponse]
  delete: [notice: NoticeResponse]
  retry: []
}>()

const board = useNoticeBoard(
  () => props.notices,
  () => emit('create'),
  () => props.canCreate,
)

const handleEdit = () => {
  if (!board.selectedNotice.value) return

  emit('edit', board.selectedNotice.value)
  board.detailDialog.value = false
}

const handleDelete = () => {
  if (!board.selectedNotice.value) return

  emit('delete', board.selectedNotice.value)
  board.deleteConfirmDialog.value = false
  board.detailDialog.value = false
}
</script>

<template>
  <v-card elevation="2" rounded="xl" class="notice-card">
    <div class="board-header">
      <div>
        <div class="text-h6 font-weight-bold">
          お知らせボード
        </div>

        <div class="text-caption text-grey">
          期限・注意事項・社内連絡を確認
        </div>
      </div>
    </div>

    <BatchPageToolbar
      :left-items="board.leftToolbarItems.value"
      :right-items="board.rightToolbarItems.value"
    />

    <v-progress-linear v-if="props.loading" indeterminate color="primary" />

    <v-alert v-if="props.error" type="error" variant="tonal" class="ma-5">
      <div class="status-alert">
        <span>お知らせの取得に失敗しました。</span>
        <v-btn variant="text" color="error" @click="emit('retry')">再試行</v-btn>
      </div>
    </v-alert>

    <template v-else-if="!props.loading">
      <NoticeBoardSummary
        :total-count="props.notices.length"
        :important-count="board.importantCount.value"
        :warning-count="board.warningCount.value"
        :pinned-count="board.pinnedCount.value"
      />

      <div class="search-area">
        <SearchPanel
          v-model="board.filter"
          :fields="board.searchFields"
          @clear="board.clearFilter"
        />
      </div>

      <v-divider />

      <NoticeBoardList
        :notices="board.pagedNotices.value"
        :page="board.page.value"
        :page-count="board.pageCount.value"
        :get-color="board.getColor"
        :get-label="board.getLabel"
        :format-period="board.formatPeriod"
        @open="board.openDetail"
        @update:page="board.page.value = $event"
      />

      <NoticeBoardDetailDialog
        v-model="board.detailDialog.value"
        v-model:delete-confirm="board.deleteConfirmDialog.value"
        :notice="board.selectedNotice.value"
        :deleting="props.deleting"
        :can-edit="
          !!board.selectedNotice.value && props.canEdit(board.selectedNotice.value)
        "
        :can-delete="
          !!board.selectedNotice.value && props.canDelete(board.selectedNotice.value)
        "
        :get-color="board.getColor"
        :get-label="board.getLabel"
        :format-period="board.formatPeriod"
        @edit="handleEdit"
        @delete="handleDelete"
      />
    </template>
  </v-card>
</template>

<style scoped>
.notice-card {
  overflow: hidden;
}

.board-header {
  padding: 16px 20px 8px;
}

.search-area {
  padding: 0 20px;
}

.status-alert {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
</style>
