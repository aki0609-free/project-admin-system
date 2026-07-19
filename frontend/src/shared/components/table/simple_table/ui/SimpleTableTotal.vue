<template>
  <div class="table-toolbar">
    <v-data-table-footer />

    <div class="summary-box">
      <!-- 件数 -->
      <div class="summary-item">
        <span class="summary-label">合計データ数</span>
        <span class="summary-value">{{ totalCount }}</span>
        <span class="summary-unit"></span>
      </div>

      <!-- 数値合計 -->
      <div
        v-for="key in sumKeys"
        :key="String(key)"
        class="summary-item"
      >
        <span class="summary-label">{{ String(key) }}</span>
        <span class="summary-value">{{ totals[key] }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts" generic="T extends SimpleTableEditableRow">
import { useSimpleTableTotal } from '@/shared/components/table/simple_table/composables/useSimpleTableTotal';
import { SimpleTableEditableRow } from '@/shared/components/table/simple_table/types/item/types';
import { computed, ComputedRef, Ref } from 'vue'

const props = defineProps<{
  filteredItems: Ref<T[]> | ComputedRef<T[]>
  sumKeys?: (keyof T)[]
}>()

const { createTotal } = useSimpleTableTotal<T>(props.filteredItems)

// 合計対象カラム
const sumKeys = computed(() => props.sumKeys ?? [])

// 件数
const totalCount = createTotal('count')

// 各カラム合計
const totals = computed(() => {
  const result = {} as Record<keyof T, number>
  for (const key of sumKeys.value) {
    result[key] = createTotal(key).value
  }
  return result
})
</script>

<style scoped>
.table-toolbar {
  position: sticky;
  left: 0;
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background: #fafafa;
  border-top: 1px solid #e0e0e0;
}

.summary-box {
  display: flex;
  align-items: baseline;
  gap: 16px;
  margin-left: auto;
}

.summary-item {
  display: flex;
  align-items: baseline;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 20px;
  background: #f0f3f7;
}

.summary-label {
  font-size: 12px;
  color: #666;
}

.summary-value {
  font-size: 18px;
  font-weight: 600;
  color: #1976d2;
}

.summary-unit {
  font-size: 12px;
  color: #888;
}
</style>