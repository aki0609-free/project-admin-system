<script setup lang="ts">
import ListDetailPageLayout from '@/toolbox/pages/ListDetailPageLayout.vue'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'

import ClosingSummaryCard from '../components/ClosingSummaryCard.vue'
import OperationReportTab from '@/features/operation/reportpreview/components/OperationReportTab.vue'

import { useMonthlyOperationPage } from '../composables/useMonthlyOperationPage'

const {
  targetMonth,
  activeTab,
  tabs,
  summary,
  leftToolbarItems,
  rightToolbarItems,
} = useMonthlyOperationPage()
</script>

<template>
  <ListDetailPageLayout
    title="月次管理"
    description="月次締め・月次帳票を確認します。"
    :left-toolbar-items="leftToolbarItems"
    :right-toolbar-items="rightToolbarItems"
  >
    <template #search>
      <div class="closing-search">
        <v-text-field
          v-model="targetMonth"
          type="month"
          label="対象月"
          variant="outlined"
          density="compact"
          hide-details
          prepend-inner-icon="mdi-calendar-month"
        />
      </div>
    </template>

    <TabLayout
      v-model="activeTab"
      :tabs="tabs"
    >
      <template #default="{ active }">

        <ClosingSummaryCard
          v-if="active === 'summary'"
          :summary="summary"
        />

        <OperationReportTab
          v-else
          operation-type="MONTHLY"
          :target-month="targetMonth"
        />

      </template>
    </TabLayout>
  </ListDetailPageLayout>
</template>

<style scoped>
.closing-search {
  max-width: 220px;
}
</style>