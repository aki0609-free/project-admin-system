<script setup lang="ts">
import { computed } from 'vue'
import { markdownToHtml } from '@/shared/utils/MarkdownUtils'
import { plainTextToHtml, sanitizeHtml } from '@/shared/utils/HtmlUtils'

type NoticeContentFormat = 'PLAIN_TEXT' | 'HTML' | 'MARKDOWN'

const props = defineProps<{
  content: string | null | undefined
  contentFormat: NoticeContentFormat | null | undefined
  emptyText?: string
}>()

const html = computed(() => {
  const content = props.content || props.emptyText || ''

  if (props.contentFormat === 'HTML') {
    return sanitizeHtml(content)
  }

  if (props.contentFormat === 'MARKDOWN') {
    return markdownToHtml(content)
  }

  return plainTextToHtml(content)
})
</script>

<template>
  <div
    class="notice-content-viewer"
    v-html="html"
  />
</template>

<style scoped>
.notice-content-viewer {
  line-height: 1.8;
  color: #334155;
  word-break: break-word;
}

.notice-content-viewer :deep(a) {
  color: #2563eb;
  text-decoration: underline;
}

.notice-content-viewer :deep(p) {
  margin: 0 0 8px;
}

.notice-content-viewer :deep(ul),
.notice-content-viewer :deep(ol) {
  padding-left: 20px;
}

.notice-content-viewer :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
}
</style>