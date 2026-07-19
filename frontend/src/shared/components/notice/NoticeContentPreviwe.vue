<script setup lang="ts">
import NoticeContentViewer from '@/shared/components/notice/NoticeContentViewer.vue'

type NoticeContentFormat = 'PLAIN_TEXT' | 'HTML' | 'MARKDOWN'

defineProps<{
  content: string | null | undefined
  contentFormat: NoticeContentFormat | null | undefined
}>()
</script>

<template>
  <v-card variant="tonal" class="preview-card">
    <v-card-title class="preview-title">
      プレビュー
    </v-card-title>

    <v-card-text>
      <NoticeContentViewer
        :content="content"
        :content-format="contentFormat"
        empty-text="プレビューする内容がありません。"
      />
    </v-card-text>
  </v-card>
</template>

<style scoped>
.preview-card {
  margin-top: 12px;
}

.preview-title {
  font-size: 13px;
  font-weight: 700;
  color: #64748b;
}
</style>