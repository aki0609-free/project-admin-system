<script setup lang="ts">
import { computed, toRef } from 'vue'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'
import FormGridTab from '@/toolbox/tab/FormGridTab.vue'
import DetailDialogLayout from '@/toolbox/dialog/DetailDialogLayout.vue'

import type { DailyReportDetailResponse } from '@/features/dailyreport/types/dailyReportApiTypes'
import type { DailyReportForm } from '@/features/dailyreport/types/dailyReportFormTypes'
import type { EmployeeListItemResponse } from '@/features/employees/types/employeeApiTypes'

import { useDailyReportEditDialog } from '@/features/dailyreport/composables/useDailyReportEditDialog'
import type { DailyReportCreateParams } from '@/features/dailyreport/composables/useDailyReportDialog'
import type { DailyReportAmountItemForm } from '@/features/dailyreport/types/dailyReportInputItemTypes'

const props = defineProps<{
  modelValue: boolean
  dailyReport: DailyReportDetailResponse | null
  createParams: DailyReportCreateParams | null
  employees: EmployeeListItemResponse[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'save' | 'delete', value: DailyReportForm): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: value =>
    emit('update:modelValue', value),
})

const {
  activeTab,
  formModel,
  isEdit,
  tabs,
  fields,
  billingFields,
  financeFields,
  schema,
  footerItems,
  billingRateLoading,
  payrollItemsLoading,
  payrollItemsError,
} = useDailyReportEditDialog(
  visible,
  toRef(props, 'dailyReport'),
  toRef(props, 'createParams'),
  toRef(props, 'employees'),
  form => emit('save', form),
  form => emit('delete', form),
)

const updateDeductionAmount = (
  item: DailyReportAmountItemForm,
  value: unknown,
) => {
  item.amount = Number(value ?? 0)
  item.manualOverride =
    (item.inputMode === 'AUTO_WITH_OVERRIDE'
      || item.inputMode === 'FIXED_WITH_OVERRIDE')
    && item.amount !== item.calculatedAmount
  if (!item.manualOverride) {
    item.overrideReason = ''
  }
}

const updateBalanceQuantity = (
  item: DailyReportAmountItemForm,
  value: unknown,
) => {
  item.quantity = Math.max(0, Number(value ?? 0))
  item.remainingAfterQuantity = Math.max(
    0,
    item.remainingQuantity - item.quantity,
  )
  if (item.code === 'DORMITORY_FEE') {
    formModel.dormitoryChargeDays = item.quantity
  }
}
</script>

<template>
  <DetailDialogLayout
    v-model="visible"
    :title="isEdit ? '日報編集' : '日報新規作成'"
    max-width="1280"
    :footer-items="footerItems"
  >
    <TabLayout
      v-model="activeTab"
      :tabs="tabs"
    >
      <template #default="{ active }">
        <FormGridTab
          v-if="active === 'basic'"
          v-model="formModel"
          :schema="schema"
          :fields="fields"
        >
          <v-textarea
            v-model="formModel.workDescription"
            label="作業内容"
            variant="outlined"
            rows="4"
            auto-grow
            hide-details
          />
        </FormGridTab>

        <div
          v-else-if="active === 'billing'"
          class="billing-panel"
        >
          <v-alert
            v-if="
              formModel.customerSiteId == null
            "
            type="info"
            variant="tonal"
          >
            基本情報タブで顧客と現場を選択してください。
          </v-alert>

          <template v-else>
            <v-alert
              type="info"
              variant="tonal"
            >
              現場・職種・現場役職・勤務日から適用単価を確認します。
              保存時にはサーバー側で再判定されます。
            </v-alert>

            <v-progress-linear
              v-if="billingRateLoading"
              indeterminate
            />

            <FormGridTab
              v-model="formModel"
              :schema="schema"
              :fields="billingFields"
            />
          </template>
        </div>

        <div
          v-else-if="active === 'allowance'"
          class="amount-panel"
        >
          <v-progress-linear
            v-if="payrollItemsLoading"
            indeterminate
          />

          <v-alert
            v-if="payrollItemsError"
            type="error"
            variant="tonal"
          >
            {{ payrollItemsError }}
          </v-alert>
          <div class="amount-panel-header">
            <div>
              <div class="amount-panel-title">
                手当
              </div>

              <div class="amount-panel-subtitle">
                日報に反映する手当金額を確認・入力します。
              </div>
            </div>
          </div>

          <div class="amount-list">
            <div
              v-for="item in formModel.allowances"
              :key="item.masterId"
              class="amount-card"
            >
              <div class="amount-info">
                <div class="amount-name">
                  {{ item.name }}
                </div>

                <div class="amount-mode">
                  {{
                    item.editable
                      ? '入力可'
                      : '自動計算・編集不可'
                  }}
                </div>
              </div>

              <v-text-field
                v-model.number="item.amount"
                type="number"
                label="金額"
                density="compact"
                variant="outlined"
                hide-details
                class="amount-input"
                :readonly="!item.editable"
              />
            </div>

            <div
              v-if="
                formModel.allowances.length === 0
              "
              class="empty-text"
            >
              表示対象の手当はありません。
            </div>
          </div>
        </div>

        <div
          v-else-if="active === 'deduction'"
          class="amount-panel"
        >
          <v-progress-linear
            v-if="payrollItemsLoading"
            indeterminate
          />

          <v-alert
            v-if="payrollItemsError"
            type="error"
            variant="tonal"
          >
            {{ payrollItemsError }}
          </v-alert>
          <div class="amount-panel-header">
            <div>
              <div class="amount-panel-title">
                控除
              </div>

              <div class="amount-panel-subtitle">
                日報に反映する控除金額を確認・入力します。
              </div>
            </div>
          </div>

          <div class="amount-list">
            <div
              v-for="item in formModel.deductions"
              :key="item.masterId"
              class="amount-card"
            >
              <div class="amount-info">
                <div class="amount-name">
                  {{ item.name }}
                </div>

                <div class="amount-mode">
                  {{
                    item.editable
                      ? '入力可'
                      : '自動計算・編集不可'
                  }}
                </div>
              </div>

              <v-text-field
                :model-value="item.amount"
                type="number"
                label="金額"
                density="compact"
                variant="outlined"
                hide-details
                class="amount-input"
                :readonly="!item.editable"
                @update:model-value="updateDeductionAmount(item, $event)"
              />

              <div
                v-if="item.inputMode === 'AUTO_WITH_OVERRIDE' || item.inputMode === 'FIXED_WITH_OVERRIDE'"
                class="amount-reference"
              >
                Rule基準額：{{ item.calculatedAmount.toLocaleString() }}円
              </div>

              <template v-if="item.balanceTracked">
                <div class="balance-section">
                  <div class="balance-summary">
                    <span>前月繰越 {{ item.openingQuantity }}</span>
                    <span>当月分 {{ item.accruedQuantity }}</span>
                    <span>現在残 {{ item.remainingQuantity }}</span>
                  </div>
                  <v-text-field
                    :model-value="item.quantity"
                    type="number"
                    :label="item.balanceUnit === 'DAYS' ? '支払い日数' : '今回の数量'"
                    density="compact"
                    variant="outlined"
                    hide-details
                    min="0"
                    :max="item.remainingQuantity"
                    @update:model-value="updateBalanceQuantity(item, $event)"
                  />
                  <div class="amount-reference">
                    {{ item.balanceUnit === 'DAYS' ? '保存後残日数' : '保存後残数' }}：{{ item.remainingAfterQuantity }} {{ item.balanceUnit === 'DAYS' ? '日' : '' }}
                  </div>
                </div>
              </template>

              <v-text-field
                v-if="item.manualOverride"
                v-model="item.overrideReason"
                class="override-reason"
                label="金額変更理由"
                density="compact"
                variant="outlined"
                maxlength="500"
                counter
                hide-details="auto"
              />
            </div>

            <div
              v-if="
                formModel.deductions.length === 0
              "
              class="empty-text"
            >
              表示対象の控除はありません。
            </div>
          </div>
        </div>

        <FormGridTab
          v-else-if="active === 'finance'"
          v-model="formModel"
          :schema="schema"
          :fields="financeFields"
        />
      </template>
    </TabLayout>
  </DetailDialogLayout>
</template>

<style scoped>
.billing-panel,
.amount-panel {
  display: grid;
  gap: 16px;
  padding: 16px;
}

.amount-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.amount-panel-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.amount-panel-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
}

.amount-list {
  display: grid;
  gap: 10px;
}

.amount-card {
  display: grid;
  grid-template-columns:
    minmax(0, 1fr)
    180px;
  align-items: center;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  box-shadow:
    0 1px 2px
    rgba(15, 23, 42, 0.04);
}

.amount-info {
  min-width: 0;
}

.amount-name {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.amount-mode {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
}

.amount-input {
  width: 180px;
}

.amount-input :deep(input) {
  text-align: right;
  font-weight: 600;
}

.amount-reference {
  grid-column: 1 / -1;
  font-size: 12px;
  color: #64748b;
}

.balance-section,
.override-reason {
  grid-column: 1 / -1;
}

.balance-section {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px;
  align-items: center;
  gap: 12px 16px;
  padding-top: 12px;
  border-top: 1px solid #e2e8f0;
}

.balance-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  font-size: 12px;
  color: #475569;
}

.empty-text {
  padding: 24px;
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
  border: 1px dashed #cbd5e1;
  border-radius: 14px;
  background: #f8fafc;
}
</style>
