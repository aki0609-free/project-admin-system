<script setup lang="ts">
import ListDetailPageLayout from '@/shared/templates/list-detail/ListDetailPageTemplate.vue'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'
import DateFormField from '@/shared/components/form/base/components/form/DateFormField.vue'
import OperationTargetFilterCard from '@/features/operation/shared/components/OperationTargetFilterCard.vue'

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
  totalPaymentAmount,

  leftToolbarItems,
  rightToolbarItems,

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
      <OperationTargetFilterCard>
        <div class="daily-payment-search">
          <DateFormField
            v-model="paymentDate"
            label="対象日"
            variant="outlined"
            density="compact"
            hide-details
            prepend-inner-icon="mdi-calendar"
          />
        </div>
      </OperationTargetFilterCard>
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
          :total-payment-amount="totalPaymentAmount"
        />

        <DailyPaymentTable
          v-else-if="active === 'details'"
          :items="rows"
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
