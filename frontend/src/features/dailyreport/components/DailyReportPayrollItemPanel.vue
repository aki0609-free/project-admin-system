<script setup lang="ts">
import { computed } from 'vue'
import type { DailyReportAmountItemForm } from '../types/dailyReportInputItemTypes'

const props = defineProps<{
  itemType: 'ALLOWANCE' | 'DEDUCTION'
  loading?: boolean
  error?: string | null
}>()

const items = defineModel<DailyReportAmountItemForm[]>({ required: true })

const itemLabel = computed(() => (props.itemType === 'ALLOWANCE' ? '手当' : '控除'))
const actionLabel = computed(() => (props.itemType === 'ALLOWANCE' ? '支給' : '支払い'))
const testIdPrefix = computed(() => props.itemType.toLowerCase())

const quantityLabel = (item: DailyReportAmountItemForm) => {
  if (item.balanceUnit === 'DAYS') return `${actionLabel.value}日数`
  return '今回の数量'
}

const balanceUnitLabel = (item: DailyReportAmountItemForm) => {
  switch (item.balanceUnit) {
    case 'DAYS':
      return '日'
    case 'HOURS':
      return '時間'
    case 'COUNT':
      return '回'
    case 'AMOUNT':
      return '円'
    default:
      return ''
  }
}

const updateAmount = (item: DailyReportAmountItemForm, value: unknown) => {
  const parsed = Number(value ?? 0)
  item.amount = Number.isFinite(parsed) ? Math.max(0, parsed) : 0
  item.manualOverride =
    (item.inputMode === 'AUTO_WITH_OVERRIDE' || item.inputMode === 'FIXED_WITH_OVERRIDE') &&
    item.amount !== item.calculatedAmount
  if (!item.manualOverride) item.overrideReason = ''
  if (item.balanceTracked && item.balanceUnit === 'AMOUNT') {
    item.quantity = item.amount
    const remaining = item.remainingQuantity - item.quantity
    item.remainingAfterQuantity = item.advanceConsumptionAllowed ? remaining : Math.max(0, remaining)
  }
}

const updateBalanceQuantity = (item: DailyReportAmountItemForm, value: unknown) => {
  item.quantity = Math.max(0, Number(value ?? 0))
  const remaining = item.remainingQuantity - item.quantity
  item.remainingAfterQuantity = item.advanceConsumptionAllowed ? remaining : Math.max(0, remaining)
}
</script>

<template>
  <div class="amount-panel">
    <v-progress-linear v-if="loading" indeterminate />
    <v-alert v-if="error" type="error" variant="tonal">
      {{ error }}
    </v-alert>

    <div class="amount-panel-header">
      <div>
        <div class="amount-panel-title">{{ itemLabel }}</div>
        <div class="amount-panel-subtitle">
          日報に反映する{{ itemLabel }}金額を確認・入力します。
        </div>
      </div>
    </div>

    <div class="amount-list">
      <div
        v-for="item in items"
        :key="item.masterId"
        class="amount-card"
        :data-testid="`daily-report-${testIdPrefix}-${item.code}`"
      >
        <div class="amount-info">
          <div class="amount-name">{{ item.name }}</div>
          <div class="amount-mode">
            {{ item.editable ? '入力可' : '自動計算・編集不可' }}
          </div>
        </div>

        <v-text-field
          :model-value="item.amount"
          type="number"
          label="金額"
          density="compact"
          variant="outlined"
          hide-details
          min="0"
          step="1"
          class="amount-input"
          :readonly="!item.editable"
          @update:model-value="updateAmount(item, $event)"
        />

        <div
          v-if="item.inputMode === 'AUTO_WITH_OVERRIDE' || item.inputMode === 'FIXED_WITH_OVERRIDE'"
          class="amount-reference"
        >
          Rule基準額：{{ item.calculatedAmount.toLocaleString() }}円
        </div>

        <div v-if="item.balanceTracked" class="balance-section">
          <div class="balance-summary">
            <span>前月繰越 {{ item.openingQuantity }}{{ balanceUnitLabel(item) }}</span>
            <span>当月分 {{ item.accruedQuantity }}{{ balanceUnitLabel(item) }}</span>
            <span>現在残 {{ item.remainingQuantity }}{{ balanceUnitLabel(item) }}</span>
          </div>
          <v-text-field
            v-if="item.balanceUnit !== 'AMOUNT'"
            :model-value="item.quantity"
            type="number"
            :label="quantityLabel(item)"
            density="compact"
            variant="outlined"
            hide-details
            min="0"
            :max="item.advanceConsumptionAllowed ? undefined : item.remainingQuantity"
            :suffix="balanceUnitLabel(item)"
            @update:model-value="updateBalanceQuantity(item, $event)"
          />
          <div class="amount-reference">
            今回{{ itemType === 'DEDUCTION' ? '徴収' : '支給' }}：{{ item.quantity
            }}{{ balanceUnitLabel(item) }} ／ 保存後残高：{{ item.remainingAfterQuantity
            }}{{ balanceUnitLabel(item) }}
          </div>
        </div>

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

      <div v-if="items.length === 0" class="empty-text">
        表示対象の{{ itemLabel }}はありません。
      </div>
    </div>
  </div>
</template>

<style scoped>
.amount-panel {
  display: grid;
  gap: 16px;
  padding: 16px;
}
.amount-panel-header {
  padding: 14px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #f8fafc;
}
.amount-panel-title {
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
}
.amount-panel-subtitle {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}
.amount-list {
  display: grid;
  gap: 10px;
}
.amount-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px;
  align-items: center;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}
.amount-info {
  min-width: 0;
}
.amount-name {
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
}
.amount-mode,
.amount-reference {
  color: #64748b;
  font-size: 12px;
}
.amount-mode {
  margin-top: 4px;
}
.amount-input {
  width: 180px;
}
.amount-input :deep(input) {
  text-align: right;
  font-weight: 600;
}
.amount-reference,
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
  color: #475569;
  font-size: 12px;
}
.empty-text {
  padding: 24px;
  border: 1px dashed #cbd5e1;
  border-radius: 14px;
  background: #f8fafc;
  color: #94a3b8;
  font-size: 13px;
  text-align: center;
}
@media (max-width: 720px) {
  .amount-card,
  .balance-section {
    grid-template-columns: minmax(0, 1fr);
  }
  .amount-input {
    width: 100%;
  }
}
</style>
