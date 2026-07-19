<script setup lang="ts">
import type { NoticeResponse } from '@/features/dashboard/types/dashboardTypes'
import NoticeContentViewer from '@/shared/components/notice/NoticeContentViewer.vue'

defineProps<{
  modelValue: boolean
  notice: NoticeResponse | null
  showActions?: boolean
  getColor: (notice: NoticeResponse) => string
  getLabel: (notice: NoticeResponse) => string
  formatPeriod: (notice: NoticeResponse) => string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  edit: []
  delete: []
}>()
</script>

<template>
  <v-dialog
    :model-value="modelValue"
    width="680"
    max-width="calc(100vw - 48px)"
    scrollable
    @update:model-value="emit('update:modelValue', $event)"
  >
    <v-card
      v-if="notice"
      rounded="xl"
      class="notice-detail-card"
    >
      <div class="detail-header">
        <div
          class="detail-icon"
          :style="{ backgroundColor: `${getColor(notice)}22`, color: getColor(notice) }"
        >
          <v-icon size="24">
            mdi-bell-outline
          </v-icon>
        </div>

        <div class="detail-title-area">
          <div class="detail-title-row">
            <v-icon
              v-if="notice.pinnedFlag"
              size="18"
              color="purple"
            >
              mdi-pin
            </v-icon>

            <h3 class="detail-title">
              {{ notice.title }}
            </h3>
          </div>

          <div class="detail-period">
            {{ formatPeriod(notice) }}
          </div>
        </div>

        <v-spacer />

        <v-chip
          :color="getColor(notice)"
          variant="tonal"
          size="small"
        >
          {{ getLabel(notice) }}
        </v-chip>

        <v-btn
          icon="mdi-close"
          variant="text"
          size="small"
          @click="emit('update:modelValue', false)"
        />
      </div>

      <v-divider />

      <v-card-text class="detail-body">
        <div class="meta-row">
          <v-chip
            size="x-small"
            variant="tonal"
            color="blue-grey"
          >
            {{ notice.contentFormat }}
          </v-chip>

          <v-chip
            size="x-small"
            variant="tonal"
            color="grey"
          >
            {{ notice.sourceType === 'AUTO' ? '自動生成' : '手動作成' }}
          </v-chip>

          <v-chip
            v-if="notice.sourceRuleCode"
            size="x-small"
            variant="tonal"
            color="indigo"
          >
            {{ notice.sourceRuleCode }}
          </v-chip>
        </div>

        <div class="content-panel">
          <div class="content-label">
            内容
          </div>

          <NoticeContentViewer
            :content="notice.content"
            :content-format="notice.contentFormat"
            empty-text="内容は登録されていません。"
          />
        </div>
      </v-card-text>

      <v-divider v-if="showActions" />

      <v-card-actions
        v-if="showActions"
        class="detail-actions"
      >
        <v-btn
          color="error"
          variant="text"
          prepend-icon="mdi-delete-outline"
          @click="emit('delete')"
        >
          削除
        </v-btn>

        <v-spacer />

        <v-btn
          variant="text"
          @click="emit('update:modelValue', false)"
        >
          閉じる
        </v-btn>

        <v-btn
          color="primary"
          variant="flat"
          prepend-icon="mdi-pencil-outline"
          @click="emit('edit')"
        >
          編集
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.notice-detail-card {
  overflow: hidden;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 22px;
  background: linear-gradient(135deg, #ffffff, #f8fafc);
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
  padding: 18px 22px;
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

.detail-actions {
  padding: 12px 18px;
  background: #ffffff;
}
</style>