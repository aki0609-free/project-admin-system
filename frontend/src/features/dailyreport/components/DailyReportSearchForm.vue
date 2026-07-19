<script setup lang="ts">
type Condition = {
  employeeId: number | null
  workDateFrom: string
  workDateTo: string
  approvalStatus: string
  keyword: string
}

type Option = {
  title: string
  value: number
}

defineProps<{
  condition: Condition
  employeeOptions: Option[]
}>()

const emit = defineEmits<{
  (e: 'update-condition', value: Partial<Condition>): void
  (e: 'clear'): void
}>()

const toNumberOrNull = (value: string): number | null =>
  value === '' ? null : Number(value)
</script>

<template>
  <div class="search-card">
    <div class="search-grid">
      <label class="field">
        <span>従業員</span>
        <select
          :value="condition.employeeId ?? ''"
          @change="
            emit('update-condition', {
              employeeId: toNumberOrNull(($event.target as HTMLSelectElement).value),
            })
          "
        >
          <option value="">すべて</option>
          <option
            v-for="employee in employeeOptions"
            :key="employee.value"
            :value="employee.value"
          >
            {{ employee.title }}
          </option>
        </select>
      </label>

      <label class="field">
        <span>勤務日From</span>
        <input
          type="date"
          :value="condition.workDateFrom"
          @input="
            emit('update-condition', {
              workDateFrom: ($event.target as HTMLInputElement).value,
            })
          "
        >
      </label>

      <label class="field">
        <span>勤務日To</span>
        <input
          type="date"
          :value="condition.workDateTo"
          @input="
            emit('update-condition', {
              workDateTo: ($event.target as HTMLInputElement).value,
            })
          "
        >
      </label>

      <label class="field">
        <span>承認状態</span>
        <select
          :value="condition.approvalStatus"
          @change="
            emit('update-condition', {
              approvalStatus: ($event.target as HTMLSelectElement).value,
            })
          "
        >
          <option value="">すべて</option>
          <option value="PENDING">未承認</option>
          <option value="APPROVED">承認済</option>
          <option value="REJECTED">却下</option>
        </select>
      </label>

      <label class="field keyword">
        <span>キーワード</span>
        <input
          type="text"
          :value="condition.keyword"
          placeholder="氏名・顧客・現場・作業内容"
          @input="
            emit('update-condition', {
              keyword: ($event.target as HTMLInputElement).value,
            })
          "
        >
      </label>

      <div class="actions">
        <button type="button" class="btn-clear" @click="emit('clear')">
          クリア
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.search-card {
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
}

.search-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr 1fr 1fr 1.6fr auto;
  gap: 12px;
  align-items: end;
}

.field {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.field span {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

.field input,
.field select {
  width: 100%;
  height: 40px;
  box-sizing: border-box;
  padding: 0 10px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #fff;
}

.actions {
  display: flex;
  justify-content: flex-end;
}

.btn-clear {
  height: 40px;
  padding: 0 14px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
}

@media (max-width: 1200px) {
  .search-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .actions {
    justify-content: flex-start;
  }
}

@media (max-width: 700px) {
  .search-grid {
    grid-template-columns: 1fr;
  }
}
</style>