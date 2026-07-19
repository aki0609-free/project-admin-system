<script setup lang="ts">
import type { NoticeResponse } from '@/features/dashboard/types/dashboardTypes'

defineProps<{
  modelValue: boolean
  selectedDate: string | null
  notices: NoticeResponse[]
  getColor: (notice: NoticeResponse) => string
  getLabel: (notice: NoticeResponse) => string
  formatPeriod: (notice: NoticeResponse) => string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  open: [notice: NoticeResponse]
}>()
</script>

<template>
  <v-dialog
    :model-value="modelValue"
    width="560"
    max-width="calc(100vw - 48px)"
    scrollable
    @update:model-value="emit('update:modelValue', $event)"
  >
    <v-card
      rounded="xl"
      class="day-dialog-card"
    >
      <div class="day-dialog-header">
        <div class="header-icon">
          <v-icon size="24">
            mdi-calendar-text-outline
          </v-icon>
        </div>

        <div class="header-title">
          <div class="text-h6 font-weight-bold">
            {{ selectedDate || '-' }}
          </div>

          <div class="text-caption text-grey">
            この日のNotice一覧
          </div>
        </div>

        <v-spacer />

        <v-chip
          size="small"
          color="primary"
          variant="tonal"
        >
          {{ notices.length }}件
        </v-chip>

        <v-btn
          icon="mdi-close"
          variant="text"
          size="small"
          @click="emit('update:modelValue', false)"
        />
      </div>

      <v-divider />

      <v-card-text class="day-dialog-body">
        <div
          v-if="notices.length > 0"
          class="notice-list"
        >
          <button
            v-for="notice in notices"
            :key="notice.id"
            type="button"
            class="notice-row"
            @click="emit('open', notice)"
          >
            <span
              class="notice-color"
              :style="{ backgroundColor: getColor(notice) }"
            />

            <span class="notice-main">
              <span class="notice-title">
                <v-icon
                  v-if="notice.pinnedFlag"
                  size="15"
                  color="purple"
                  class="mr-1"
                >
                  mdi-pin
                </v-icon>

                {{ notice.title }}
              </span>

              <span class="notice-period">
                {{ formatPeriod(notice) }}
              </span>
            </span>

            <span class="notice-meta">
              <v-chip
                size="x-small"
                variant="tonal"
                :color="getColor(notice)"
              >
                {{ getLabel(notice) }}
              </v-chip>

              <v-chip
                v-if="notice.sourceType === 'AUTO'"
                size="x-small"
                variant="tonal"
                color="grey"
              >
                自動
              </v-chip>

              <v-icon
                size="18"
                color="grey"
              >
                mdi-chevron-right
              </v-icon>
            </span>
          </button>
        </div>

        <div
          v-else
          class="empty-area"
        >
          <v-icon
            size="42"
            color="grey"
          >
            mdi-calendar-blank-outline
          </v-icon>

          <div class="mt-2">
            この日のNoticeはありません。
          </div>
        </div>
      </v-card-text>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.day-dialog-card {
  overflow: hidden;
}

.day-dialog-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  background: linear-gradient(135deg, #ffffff, #f8fafc);
}

.header-icon {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: #dbeafe;
  color: #1d4ed8;
  flex: 0 0 auto;
}

.header-title {
  min-width: 0;
}

.day-dialog-body {
  padding: 14px;
  background: #ffffff;
}

.notice-list {
  display: grid;
  gap: 10px;
}

.notice-row {
  width: 100%;
  display: grid;
  grid-template-columns: 5px minmax(0, 1fr) auto;
  align-items: stretch;
  gap: 12px;
  padding: 0;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #ffffff;
  overflow: hidden;
  cursor: pointer;
  text-align: left;
  transition:
    background-color 0.2s,
    transform 0.2s,
    box-shadow 0.2s;
}

.notice-row:hover {
  background: #f8fafc;
  transform: translateY(-1px);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
}

.notice-color {
  min-height: 100%;
}

.notice-main {
  min-width: 0;
  display: grid;
  gap: 4px;
  padding: 12px 0;
}

.notice-title {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-period {
  font-size: 12px;
  color: #64748b;
}

.notice-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 12px 0 0;
}

.empty-area {
  min-height: 220px;
  display: grid;
  place-items: center;
  align-content: center;
  color: #64748b;
  text-align: center;
}
</style>