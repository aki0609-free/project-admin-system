<script setup lang="ts" generic="T">
import type { MultiLevelHeaderLeafColumn } from '../types/item/types'

const props = defineProps<{
  summary: Record<string, unknown>
  columns: MultiLevelHeaderLeafColumn<T>[]
  rowTitle?: string
}>()
</script>

<template>
  <tfoot class="mlh-tfoot">
    <tr>
      <!-- 左固定列 -->
      <td class="mlh-td mlh-row-header">
        {{ rowTitle ?? '合計' }}
      </td>

      <!-- 各列 -->
      <td
        v-for="col in columns"
        :key="String(col.key)"
        class="mlh-td"
      >
        <span v-if="summary[col.key as string] !== ''">
          {{
            col.formatter
              ? col.formatter(summary[col.key as string], {} as T)
              : summary[col.key as string]
          }}
        </span>
      </td>
    </tr>
  </tfoot>
</template>

<style scoped>
.mlh-tfoot {
  position: sticky;
  bottom: 0;
  background: #fff;
  z-index: 4;
  border-top: 2px solid #e5e7eb;
  font-weight: 600;
}
</style>