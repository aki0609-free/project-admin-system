<script setup lang="ts">
import type { NoticeResponse } from '@/features/dashboard/types/dashboardTypes'

defineProps<{
  notices: NoticeResponse[]
  page: number
  pageCount: number
  getColor: (notice: NoticeResponse) => string
  getLabel: (notice: NoticeResponse) => string
  formatPeriod: (notice: NoticeResponse) => string
}>()

const emit = defineEmits<{
  open: [notice: NoticeResponse]
  'update:page': [value: number]
}>()
</script>

<template>
  <v-card-text class="pa-0">
    <template v-if="notices.length > 0">
      <v-list class="py-0">
        <template
          v-for="(notice, index) in notices"
          :key="notice.id"
        >
          <v-list-item
            class="notice-item"
            @click="emit('open', notice)"
          >
            <template #prepend>
              <v-avatar
                size="12"
                :color="getColor(notice)"
              />
            </template>

            <div class="notice-content">
              <div class="notice-title-row">
                <v-icon
                  v-if="notice.pinnedFlag"
                  size="16"
                  color="purple"
                >
                  mdi-pin
                </v-icon>

                <span class="notice-title">
                  {{ notice.title }}
                </span>

                <v-chip
                  size="x-small"
                  :color="getColor(notice)"
                  variant="tonal"
                >
                  {{ getLabel(notice) }}
                </v-chip>

                <v-chip
                  v-if="notice.sourceType === 'AUTO'"
                  size="x-small"
                  color="grey"
                  variant="tonal"
                >
                  自動
                </v-chip>
              </div>

              <div class="notice-period">
                {{ formatPeriod(notice) }}
              </div>
            </div>

            <template #append>
              <v-btn
                icon="mdi-eye-outline"
                variant="text"
                size="small"
              />
            </template>
          </v-list-item>

          <v-divider v-if="index !== notices.length - 1" />
        </template>
      </v-list>

      <div class="pagination-area">
        <v-pagination
          :model-value="page"
          :length="pageCount"
          density="comfortable"
          rounded="circle"
          @update:model-value="emit('update:page', $event)"
        />
      </div>
    </template>

    <div v-else class="empty-area">
      <v-icon size="32" color="primary">
        mdi-bell-outline
      </v-icon>

      <div class="mt-2 text-body-2 text-primary">
        条件に一致するお知らせはありません
      </div>
    </div>
  </v-card-text>
</template>

<style scoped>
.notice-item {
  cursor: pointer;
  min-height: 76px;
  transition: background-color 0.2s;
}

.notice-item:hover {
  background-color: #eff6ff;
}

.notice-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}

.notice-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notice-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.notice-period {
  font-size: 12px;
  color: #2563eb;
}

.pagination-area {
  display: flex;
  justify-content: center;
  padding: 12px 0 16px;
  background: #ffffff;
}

.empty-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 220px;
}
</style>