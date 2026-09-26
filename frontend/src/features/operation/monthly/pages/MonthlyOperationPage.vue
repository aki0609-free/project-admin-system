<script setup lang="ts">
import ListDetailPageLayout from '@/shared/templates/list-detail/ListDetailPageTemplate.vue'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'
import MonthFormField from '@/shared/components/form/base/components/form/MonthFormField.vue'
import OperationTargetFilterCard from '@/features/operation/shared/components/OperationTargetFilterCard.vue'

import ClosingSummaryCard from '../components/ClosingSummaryCard.vue'
import OperationReportTab from '@/features/operation/reportpreview/components/OperationReportTab.vue'

import { useMonthlyOperationPage } from '../composables/useMonthlyOperationPage'

const {
  targetMonth,
  activeTab,
  tabs,
  summary,
  errorMessage,
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
      <OperationTargetFilterCard>
        <div class="closing-search">
          <MonthFormField
            v-model="targetMonth"
            label="対象月"
            variant="outlined"
            density="compact"
            hide-details
            prepend-inner-icon="mdi-calendar-month"
          />
        </div>
      </OperationTargetFilterCard>
    </template>

    <v-alert
      v-if="errorMessage"
      type="error"
      variant="tonal"
      class="mb-4"
      closable
      @click:close="errorMessage = ''"
    >
      {{ errorMessage }}
    </v-alert>

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
          :closing-version="summary?.closing?.closingVersion ?? null"
          :excluded-report-codes="['MONTHLY_INVOICE', 'MONTHLY_ORDER_FORM']"
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
