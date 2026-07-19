<script setup lang="ts">
import ListDetailPageLayout from '@/toolbox/pages/ListDetailPageLayout.vue'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'

import DailyPaymentSummaryCard from '../components/DailyPaymentSummaryCard.vue'
import DailyPaymentTable from '../components/DailyPaymentTable.vue'
import OperationReportTab from '@/features/operation/reportpreview/components/OperationReportTab.vue'
import { useDailyOperationPage } from '../composables/useDailyOperationPage'

const {
  paymentDate,
  activeTab,
  tabs,

  rows,
  countText,

  employeeCount,
  totalPlannedAmount,
  totalActualAmount,

  leftToolbarItems,
  rightToolbarItems,

  handleCellUpdate,
} = useDailyOperationPage()
</script>

<template>
  <ListDetailPageLayout
    title="日次管理"
    description="日報集計・日次帳票を確認します。"
    :left-toolbar-items="leftToolbarItems"
    :right-toolbar-items="rightToolbarItems"
  >
    <template #search>
      <div class="daily-payment-search">
        <v-text-field
          v-model="paymentDate"
          type="date"
          label="対象日"
          variant="outlined"
          density="compact"
          hide-details
          prepend-inner-icon="mdi-calendar"
        />
      </div>
    </template>

    <template #before-table>
      <p class="count-text">
        {{ countText }}
      </p>
    </template>

    <TabLayout
      v-model="activeTab"
      :tabs="tabs"
    >
      <template #default="{ active }">
        <DailyPaymentSummaryCard
          v-if="active === 'summary'"
          :employee-count="employeeCount"
          :total-planned-amount="totalPlannedAmount"
          :total-actual-amount="totalActualAmount"
        />

        <DailyPaymentTable
          v-else-if="active === 'details'"
          :items="rows"
          @update:items="handleCellUpdate"
        />

        <OperationReportTab
          v-else
          operation-type="DAILY"
          :target-date="paymentDate"
        />
      </template>
    </TabLayout>
  </ListDetailPageLayout>
</template>

<style scoped>
.daily-payment-search {
  max-width: 240px;
}

.count-text {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}
</style>