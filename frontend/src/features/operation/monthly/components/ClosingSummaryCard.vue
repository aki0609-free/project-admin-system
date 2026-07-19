<script setup lang="ts">
import type { MonthlyClosingSummaryResponse } from '../types/closingApiTypes'

defineProps<{
  summary: MonthlyClosingSummaryResponse | null
}>()

const money = (value: number | null | undefined) => `${Number(value ?? 0).toLocaleString()}円`

const statusLabel = (status: string | undefined) => {
  if (status === 'CLOSED') return '締め済み'
  if (status === 'OPEN') return '未締め'
  return '未作成'
}
</script>

<template>
  <div v-if="!summary" class="empty">月次集計データがありません。</div>

  <div v-else class="summary-grid">
    <v-card variant="outlined" class="summary-card">
      <div class="label">対象月</div>
      <div class="value">{{ summary.targetMonth }}</div>
    </v-card>

    <v-card variant="outlined" class="summary-card">
      <div class="label">締め状態</div>
      <div class="value">
        {{ statusLabel(summary.closing?.status) }}
      </div>
    </v-card>

    <v-card variant="outlined" class="summary-card">
      <div class="label">従業員数</div>
      <div class="value">{{ summary.employeeCount }} 人</div>
    </v-card>

    <v-card variant="outlined" class="summary-card">
      <div class="label">日報件数</div>
      <div class="value">{{ summary.workReportCount }} 件</div>
    </v-card>

    <v-card variant="outlined" class="summary-card">
      <div class="label">総支給額</div>
      <div class="value">{{ money(summary.totalGrossAmount) }}</div>
    </v-card>

    <v-card variant="outlined" class="summary-card">
      <div class="label">控除合計</div>
      <div class="value">{{ money(summary.totalDeductionAmount) }}</div>
    </v-card>

    <v-card variant="outlined" class="summary-card">
      <div class="label">日払い合計</div>
      <div class="value">{{ money(summary.totalDailyPaymentAmount) }}</div>
    </v-card>

    <v-card variant="outlined" class="summary-card highlight">
      <div class="label">月末支払見込</div>
      <div class="value">{{ money(summary.totalNetPaymentAmount) }}</div>
    </v-card>

    <v-card variant="outlined" class="summary-card">
      <div class="label">締めVersion</div>
      <div class="value">V{{ summary.closing?.closingVersion ?? 0 }}</div>
    </v-card>

    <v-card variant="outlined" class="summary-card">
      <div class="label">締め期間</div>
      <div class="value-small">
        {{ summary.closing?.closingStartDate }}
        ～<br />
        {{ summary.closing?.closingEndDate }}
      </div>
    </v-card>
  </div>
</template>

<style scoped>
.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.summary-card {
  padding: 16px;
  border-radius: 12px;
}

.label {
  color: #64748b;
  font-size: 12px;
}

.value {
  margin-top: 4px;
  font-size: 20px;
  font-weight: 800;
}

.highlight {
  background: #f8fafc;
}

.empty {
  padding: 24px;
  color: #64748b;
  text-align: center;
  border: 1px dashed #cbd5e1;
  border-radius: 12px;
}
</style>
