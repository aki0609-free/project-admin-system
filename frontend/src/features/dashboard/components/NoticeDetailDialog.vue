<script setup lang="ts">
import { computed } from 'vue'
import type { NoticeResponse } from '@/features/dashboard/types/dashboardTypes'
import NoticeContentViewer from '@/shared/components/notice/NoticeContentViewer.vue'
import AppDialog from '@/shared/ui/dialog/AppDialog.vue'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'

const props = defineProps<{
  modelValue: boolean
  notice: NoticeResponse | null
  showActions?: boolean
  canEdit?: boolean
  canDelete?: boolean
  getColor: (notice: NoticeResponse) => string
  getLabel: (notice: NoticeResponse) => string
  formatPeriod: (notice: NoticeResponse) => string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  edit: []
  delete: []
}>()

const close = () => emit('update:modelValue', false)

const leftFooterItems = computed<ToolbarItem[]>(() =>
  props.showActions && props.canDelete
    ? [
        {
          type: 'button',
          label: '削除',
          intent: 'danger',
          onClick: () => emit('delete'),
        },
      ]
    : [],
)

const rightFooterItems = computed<ToolbarItem[]>(() => {
  if (!props.showActions || (!props.canEdit && !props.canDelete)) return []

  const items: ToolbarItem[] = [
    {
      type: 'button',
      label: '閉じる',
      intent: 'utility',
      onClick: close,
    },
  ]

  if (props.canEdit) {
    items.push({
      type: 'button',
      label: '編集',
      intent: 'primary',
      onClick: () => emit('edit'),
    })
  }

  return items
})
</script>

<template>
  <AppDialog
    :model-value="modelValue"
    :title="notice?.title || 'お知らせ詳細'"
    size="md"
    :max-width="680"
    closable
    :left-footer-items="leftFooterItems"
    :right-footer-items="rightFooterItems"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <template v-if="notice" #title>
      <div class="title-content">
        <div
          class="detail-icon"
          :style="{ backgroundColor: `${getColor(notice)}22`, color: getColor(notice) }"
        >
          <v-icon size="24"> mdi-bell-outline </v-icon>
        </div>

        <div class="detail-title-area">
          <div class="detail-title-row">
            <v-icon v-if="notice.pinnedFlag" size="18" color="purple"> mdi-pin </v-icon>

            <h2 class="detail-title">{{ notice.title }}</h2>
          </div>

          <div class="detail-period">
            {{ formatPeriod(notice) }}
          </div>
        </div>
      </div>
    </template>

    <template v-if="notice" #header-actions>
      <v-chip :color="getColor(notice)" variant="tonal" size="small">
        {{ getLabel(notice) }}
      </v-chip>
    </template>

    <div v-if="notice" class="detail-body">
      <div class="meta-row">
        <v-chip size="x-small" variant="tonal" color="blue-grey">
          {{ notice.contentFormat }}
        </v-chip>

        <v-chip size="x-small" variant="tonal" color="grey">
          {{ notice.sourceType === 'AUTO' ? '自動生成' : '手動作成' }}
        </v-chip>

        <v-chip v-if="notice.sourceRuleCode" size="x-small" variant="tonal" color="indigo">
          {{ notice.sourceRuleCode }}
        </v-chip>
      </div>

      <div class="content-panel">
        <div class="content-label">内容</div>

        <NoticeContentViewer
          :content="notice.content"
          :content-format="notice.contentFormat"
          empty-text="内容は登録されていません。"
        />
      </div>
    </div>
  </AppDialog>
</template>

<style scoped>
.title-content {
  display: flex;
  align-items: center;
  gap: 14px;
}

.detail-icon {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
}

.detail-title-area {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.detail-title-row {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.detail-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-period {
  font-size: 12px;
  color: #64748b;
}

.detail-body {
  display: grid;
  gap: 14px;
  background: #ffffff;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.content-panel {
  display: grid;
  gap: 8px;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid #e5e7eb;
  background: #f8fafc;
}

.content-label {
  font-size: 12px;
  color: #2563eb;
  font-weight: 700;
}
</style>
