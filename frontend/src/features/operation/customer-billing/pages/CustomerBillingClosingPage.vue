<script setup lang="ts">
import ListDetailPageLayout from '@/shared/templates/list-detail/ListDetailPageTemplate.vue'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'
import OperationReportTab from '@/features/operation/reportpreview/components/OperationReportTab.vue'
import MonthFormField from '@/shared/components/form/base/components/form/MonthFormField.vue'
import { formatYearMonthDay } from '@/shared/utils/DateUtils'
import OperationTargetFilterCard from '@/features/operation/shared/components/OperationTargetFilterCard.vue'

import { useCustomerBillingClosingPage } from '../composables/useCustomerBillingClosingPage'

const {
  targetMonth,
  activeTab,
  tabs,
  summary,
  selectedReportCustomerId,
  selectedReportCustomer,
  loading,
  errorMessage,
  leftToolbarItems,
  rightToolbarItems,
  executeCustomer,
  isCustomerLoading,
} = useCustomerBillingClosingPage()

const money = (value: number) => `${Number(value ?? 0).toLocaleString()}円`
</script>

<template>
  <ListDetailPageLayout
    title="顧客請求締め"
    description="顧客ごとの締日で請求書・注文書・請求額を確定します。"
    :left-toolbar-items="leftToolbarItems"
    :right-toolbar-items="rightToolbarItems"
  >
    <template #search>
      <OperationTargetFilterCard>
        <div class="month-selector">
          <MonthFormField
            v-model="targetMonth"
            label="対象請求月"
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

    <TabLayout v-model="activeTab" :tabs="tabs">
      <template #default="{ active }">
        <div v-if="active === 'customers'">
          <v-alert type="info" variant="tonal" class="mb-4">
            締日当日に操作する必要はありません。対象期間は顧客マスターの締日から自動計算されます。
          </v-alert>

          <div class="status-row">
            <v-chip :color="summary?.status === 'CLOSED' ? 'success' : 'default'">
              {{ summary?.status === 'CLOSED'
                ? '全顧客締め済み'
                : summary?.status === 'PARTIALLY_CLOSED'
                  ? '一部締め済み'
                  : summary?.status === 'TARGET_NONE'
                    ? '対象なし'
                    : '未締め' }}
            </v-chip>
            <span>
              締め済み {{ summary?.closedCount ?? 0 }} / {{ summary?.targetCount ?? 0 }}社
              （本日締め可能 {{ summary?.eligibleCount ?? 0 }}社）
            </span>
          </div>

          <v-data-table
            :loading="loading"
            :items="summary?.customers ?? []"
            :headers="[
              { title: '顧客', key: 'customerName' },
              { title: '締日', key: 'closingRuleLabel' },
              { title: '集計開始', key: 'periodFrom' },
              { title: '集計終了', key: 'periodTo' },
              { title: '税抜', key: 'subtotalAmount', align: 'end' },
              { title: '消費税', key: 'taxAmount', align: 'end' },
              { title: '税込', key: 'totalAmount', align: 'end' },
              { title: '計算', key: 'calculationReady', align: 'center' },
              { title: '締め状態', key: 'closing', align: 'center' },
              { title: '操作', key: 'actions', align: 'center', sortable: false },
            ]"
            item-value="customerId"
          >
            <template #[`item.periodFrom`]="{ value }">
              {{ formatYearMonthDay(value) }}
            </template>
            <template #[`item.periodTo`]="{ value }">
              {{ formatYearMonthDay(value) }}
            </template>
            <template #[`item.subtotalAmount`]="{ value }">{{ money(value) }}</template>
            <template #[`item.taxAmount`]="{ value }">{{ money(value) }}</template>
            <template #[`item.totalAmount`]="{ value }">{{ money(value) }}</template>
            <template #[`item.calculationReady`]="{ value }">
              <v-chip size="small" :color="value ? 'success' : 'error'">
                {{ value ? '計算可能' : '設定確認' }}
              </v-chip>
            </template>
            <template #[`item.closing`]="{ item }">
              <v-chip
                size="small"
                :color="item.closing?.status === 'CLOSED'
                  ? 'success'
                  : item.closingDateReached ? 'warning' : 'default'"
              >
                {{ item.closing?.status === 'CLOSED'
                  ? `締め済み V${item.closing.closingVersion}`
                  : item.closingDateReached ? '締め可能' : '締日前' }}
              </v-chip>
            </template>
            <template #[`item.actions`]="{ item }">
              <v-btn
                size="small"
                color="primary"
                variant="tonal"
                :loading="isCustomerLoading(item.customerId)"
                :disabled="loading"
                @click.stop="executeCustomer(item)"
              >
                {{ item.closing?.status === 'CLOSED' ? '再締め' : '締め' }}
              </v-btn>
            </template>
            <template #no-data>請求対象の日報がありません。</template>
          </v-data-table>
        </div>

        <div v-else class="report-tab-content">
          <v-alert type="info" variant="tonal">
            簡易プレビューは選択顧客の最新データを顧客固有の締め期間で表示します。
            本印刷はその顧客の締め処理時に保存した確定版を使用します。
          </v-alert>

          <div class="report-customer-filter">
            <v-select
              v-model="selectedReportCustomerId"
              :items="summary?.customers ?? []"
              item-title="customerName"
              item-value="customerId"
              label="プレビュー・印刷対象の顧客"
              variant="outlined"
              density="compact"
              hide-details
              :disabled="loading || !summary?.customers.length"
            />
            <div v-if="selectedReportCustomer" class="report-period">
              集計期間:
              {{ formatYearMonthDay(selectedReportCustomer.periodFrom) }} ～
              {{ formatYearMonthDay(selectedReportCustomer.periodTo) }}
            </div>
          </div>

          <OperationReportTab
            operation-type="MONTHLY"
            :target-month="targetMonth"
            :customer-id="selectedReportCustomer?.customerId ?? null"
            :period-from="selectedReportCustomer?.periodFrom ?? null"
            :period-to="selectedReportCustomer?.periodTo ?? null"
            :closing-version="null"
            allow-mixed-closing-versions
            :allowed-report-codes="['MONTHLY_INVOICE', 'MONTHLY_ORDER_FORM']"
          />
        </div>
      </template>
    </TabLayout>
  </ListDetailPageLayout>
</template>

<style scoped>
.month-selector { max-width: 220px; }
.status-row { display: flex; gap: 16px; align-items: center; margin-bottom: 16px; }
.report-tab-content { display: grid; gap: 16px; }
.report-customer-filter {
  display: grid;
  grid-template-columns: minmax(280px, 520px) 1fr;
  gap: 16px;
  align-items: center;
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #fff;
}
.report-period { color: #475569; font-size: 14px; }
@media (max-width: 800px) {
  .report-customer-filter { grid-template-columns: 1fr; }
}
</style>
