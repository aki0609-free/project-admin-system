<script setup lang="ts">
import type { NoticeResponse } from '@/features/dashboard/types/dashboardTypes'
import { useNoticeBoard } from '@/features/dashboard/composables/useNoticeBoard'
import NoticeBoardDetailDialog from '@/features/dashboard/components/NoticeBoardDetailDialog.vue'
import NoticeBoardList from '@/features/dashboard/components/NoticeBoardList.vue'
import NoticeBoardSummary from '@/features/dashboard/components/NoticeBoardSummary.vue'
import SearchPanel from '@/shared/components/search/SearchPanel.vue'
import MultiPositionGenericToolbar from '@/shared/components/toolbar/MultiPositionGenericToolbar.vue'

const props = defineProps<{
  notices: NoticeResponse[]
}>()

const emit = defineEmits<{
  create: []
  edit: [notice: NoticeResponse]
  delete: [notice: NoticeResponse]
}>()

const board = useNoticeBoard(
  () => props.notices,
  () => emit('create'),
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

    <MultiPositionGenericToolbar
      :left-items="board.leftToolbarItems.value"
      :right-items="board.rightToolbarItems.value"
    />

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
      :get-color="board.getColor"
      :get-label="board.getLabel"
      :format-period="board.formatPeriod"
      @edit="handleEdit"
      @delete="handleDelete"
    />
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
</style>