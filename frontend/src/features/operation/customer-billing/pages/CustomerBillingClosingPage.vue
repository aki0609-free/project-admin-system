<script setup lang="ts">
import ListDetailPageLayout from '@/toolbox/pages/ListDetailPageLayout.vue'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'
import OperationReportTab from '@/features/operation/reportpreview/components/OperationReportTab.vue'

import { useCustomerBillingClosingPage } from '../composables/useCustomerBillingClosingPage'

const {
  targetMonth,
  activeTab,
  tabs,
  summary,
  loading,
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
      <div class="month-selector">
        <v-text-field
          v-model="targetMonth"
          type="month"
          label="対象請求月"
          variant="outlined"
          density="compact"
          hide-details
          prepend-inner-icon="mdi-calendar-month"
        />
      </div>
    </template>

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

        <OperationReportTab
          v-else
          operation-type="MONTHLY"
          :target-month="targetMonth"
          :closing-version="null"
          allow-mixed-closing-versions
          :allowed-report-codes="['MONTHLY_INVOICE', 'MONTHLY_ORDER_FORM']"
        />
      </template>
    </TabLayout>
  </ListDetailPageLayout>
</template>

<style scoped>
.month-selector { max-width: 220px; }
.status-row { display: flex; gap: 16px; align-items: center; margin-bottom: 16px; }
</style>
