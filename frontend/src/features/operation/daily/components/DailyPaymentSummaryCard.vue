<script setup lang="ts">
defineProps<{
  employeeCount: number
  totalPlannedAmount: number
  totalActualAmount: number
}>()

const money = (value: number) =>
  `${Number(value ?? 0).toLocaleString()}円`
</script>

<template>
  <div class="summary-card">
    <div class="summary-item">
      <div class="label">対象人数</div>
      <div class="value">{{ employeeCount }} 人</div>
    </div>

    <div class="summary-item">
      <div class="label">予定合計</div>
      <div class="value">{{ money(totalPlannedAmount) }}</div>
    </div>

    <div class="summary-item highlight">
      <div class="label">実支払合計</div>
      <div class="value">{{ money(totalActualAmount) }}</div>
    </div>
  </div>
</template>

<style scoped>
.summary-card {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  padding: 14px 16px;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  background: #ffffff;
}

.label {
  color: #64748b;
  font-size: 12px;
}

.value {
  margin-top: 4px;
  font-size: 20px;
  font-weight: 800;
  color: #0f172a;
}

.highlight {
  background: #f8fafc;
}

@media print {
  .summary-card {
    grid-template-columns: repeat(3, 1fr);
  }

  .summary-item {
    border-radius: 0;
  }
}
</style>