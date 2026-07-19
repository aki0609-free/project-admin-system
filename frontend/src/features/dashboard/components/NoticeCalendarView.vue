<script setup lang="ts">
import { toRef } from 'vue'

import type { NoticeResponse } from '@/features/dashboard/types/dashboardTypes'
import { useNoticeCalendar } from '@/features/dashboard/composables/useNoticeCalendar'
import NoticeCalendarDayDialog from '@/features/dashboard/components/NoticeCalendarDayDialog.vue'
import NoticeDetailDialog from '@/features/dashboard/components/NoticeDetailDialog.vue'

const props = defineProps<{
  notices: NoticeResponse[]
  selectedNotice: NoticeResponse | null
}>()

const calendar = useNoticeCalendar(toRef(props, 'notices'), toRef(props, 'selectedNotice'))
</script>

<template>
  <v-card elevation="3" rounded="xl" class="calendar-card">
    <v-card-title class="calendar-header">
      <div class="header-left">
        <div class="text-h5 font-weight-bold">お知らせカレンダー</div>

        <div class="text-caption text-grey">お知らせをカレンダーで確認</div>
      </div>

      <div class="header-center">
        <v-btn
          icon="mdi-chevron-left"
          variant="tonal"
          size="small"
          @click="calendar.movePrevious"
        />

        <div class="month-title">
          {{ calendar.titleText.value }}
        </div>

        <v-btn icon="mdi-chevron-right" variant="tonal" size="small" @click="calendar.moveNext" />

        <v-btn variant="flat" color="primary" size="small" class="ml-2" @click="calendar.moveToday">
          今日
        </v-btn>
      </div>

      <div class="header-right">
        <v-chip color="primary" variant="tonal"> 月表示 </v-chip>
      </div>
    </v-card-title>

    <v-divider />

    <v-card-text class="calendar-body">
      <v-calendar
        :key="calendar.calendarDate.value"
        v-model="calendar.calendarDate.value"
        class="calendar-full"
        view-mode="month"
        :events="calendar.calendarEvents.value"
        color="primary"
        @click:date="calendar.onClickDate"
      >
        <template #event="{ event }">
          <div
            class="calendar-event"
            :style="{ backgroundColor: String(event.color || '#1976d2') }"
            @click.stop="calendar.openDayDialog(calendar.formatDate(event.start as Date))"
          >
            {{ event.title }}
          </div>
        </template>
      </v-calendar>
    </v-card-text>

    <NoticeCalendarDayDialog
      v-model="calendar.dayDialog.value"
      :selected-date="calendar.selectedDate.value"
      :notices="calendar.selectedNotices.value"
      :get-color="calendar.getColor"
      :get-label="calendar.getLabel"
      :format-period="calendar.formatPeriod"
      @open="calendar.openNoticeDetail"
    />

    <NoticeDetailDialog
      v-model="calendar.detailDialog.value"
      :notice="calendar.selectedNoticeDetail.value"
      :get-color="calendar.getColor"
      :get-label="calendar.getLabel"
      :format-period="calendar.formatPeriod"
      :show-actions="false"
    />
  </v-card>
</template>

<style scoped>
.calendar-card {
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.calendar-header {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  padding: 18px 24px;
  background: linear-gradient(135deg, #ffffff, #f7f9fc);
}

.header-center {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.header-right {
  display: flex;
  justify-content: flex-end;
}

.month-title {
  min-width: 180px;
  text-align: center;
  font-size: 20px;
  font-weight: 700;
}

.calendar-body {
  flex: 1;
  min-height: 0;
  padding: 12px;
  background: #f5f7fb;
}

.calendar-full {
  height: 100%;
  border-radius: 18px;
  overflow: hidden;
  background: white;
}

.calendar-event {
  color: white;
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  cursor: pointer;
}
</style>
