<script setup lang="ts">
import { ref } from 'vue'
import CardLayout from '@/shared/components/layout/card_layout/CardLayout.vue'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'

import ApplicationMediaTableTab from '@/features/application/components/application_media/ApplicationMediaTableTab.vue'
import ApplicationMediaChartTab from '@/features/application/components/application_media/ApplicationMediaChartTab.vue'
import ApplicationMediaAnalysisTab from '@/features/application/components/application_media/ApplicationMediaAnalysisTab.vue'

import { useApplicationMediaSource } from '@/features/application/composables/application_media/useApplicationMediaSource'
import { useApplicationMediaPivot } from '@/features/application/composables/application_media/useApplicationMediaPivot'

const tabs = [
  { label: 'テーブル', value: 'table' },
  { label: 'チャート', value: 'chart' },
  { label: '分析', value: 'analysis' },
]

const activeTab = ref('table')

const {
  medias,
  isDirty,
  saving,
  markDirty,
  saveChanges,
  saveMessage,
  saveSuccess,
  showSaveAlert,
  closeSaveAlert,
} = useApplicationMediaSource()

const {
  yearMonths,
  allYearMonthOptions,
  mediaNames,
  pivotTableData,
  filter,
  handleCellUpdate,
  addDraftMedia,
  addDraftYearMonth,
  deleteMedia,
  deleteYearMonth,
} = useApplicationMediaPivot(medias, markDirty)
</script>

<template>
  <CardLayout title="応募媒体管理">
    <TabLayout v-model="activeTab" :tabs="tabs">
      <template #default="{ active }">
        <v-alert
          v-if="showSaveAlert"
          :type="saveSuccess ? 'success' : 'error'"
          variant="tonal"
          closable
          class="mb-4"
          @click:close="closeSaveAlert"
        >
          {{ saveMessage }}
        </v-alert>
        <ApplicationMediaTableTab
          v-if="active === 'table'"
          :year-months="yearMonths"
          :all-year-month-options="allYearMonthOptions"
          :media-names="mediaNames"
          :filter="filter"
          :pivot-table-data="pivotTableData"
          :is-dirty="isDirty"
          :saving="saving"
          :on-save="saveChanges"
          @update-cell="handleCellUpdate"
          @add-media="addDraftMedia"
          @add-year-month="addDraftYearMonth"
          @delete-media="deleteMedia"
          @delete-year-month="deleteYearMonth"
        />

        <ApplicationMediaChartTab v-else-if="active === 'chart'" :medias="medias" />

        <ApplicationMediaAnalysisTab v-else-if="active === 'analysis'" :medias="medias" />
      </template>
    </TabLayout>
  </CardLayout>
</template>
