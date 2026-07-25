<script setup lang="ts">
import { toRef } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import type { BatchExecutionLogResponse } from '@/features/system/batch/types/batchApiTypes'
import {
  useBatchExecutionLogTableConfig,
} from '@/features/system/batch/composables/useBatchExecutionLogTableConfig'

const props = defineProps<{
  items: BatchExecutionLogResponse[]
  busy?: boolean
}>()

const emit = defineEmits<{
  (e: 'retry' | 'download', item: BatchExecutionLogResponse): void
}>()

const {
  rows,
  columns,
  filterRules,
} = useBatchExecutionLogTableConfig(toRef(props, 'items'))
</script>

<template>
  <SimpleTable
    table-key="batch-execution-log-list"
    item-key="id"
    :items="rows"
    :columns="columns"
    :filter-rules="filterRules"
  >
    <template #[`item.status`]="{ item }">
      <v-chip
        size="small"
        variant="tonal"
        :color="item.statusColor"
      >
        {{ item.status }}
      </v-chip>
    </template>

    <template #[`item.storageType`]="{ item }">
      <v-chip
        v-if="item.storageType"
        size="small"
        variant="outlined"
      >
        {{ item.storageType }}
      </v-chip>

      <span v-else class="empty-text">
        -
      </span>
    </template>

    <template #[`item.outputFileName`]="{ item }">
      <span v-if="item.outputFileName">
        {{ item.outputFileName }}
      </span>

      <span v-else class="empty-text">
        -
      </span>
    </template>

    <template #[`item.actions`]="{ item }">
      <div class="action-buttons">
        <v-btn
          v-if="item.raw.status === 'FAILED'"
          size="small"
          variant="tonal"
          color="warning"
          :disabled="busy"
          @click.stop="emit('retry', item.raw)"
        >
          再実行
        </v-btn>

        <v-btn
          v-if="item.raw.outputFileKey"
          size="small"
          variant="text"
          color="primary"
          :disabled="busy"
          @click.stop="emit('download', item.raw)"
        >
          ダウンロード
        </v-btn>
      </div>
    </template>
  </SimpleTable>
</template>

<style scoped>
.empty-text {
  color: #94a3b8;
}

.action-buttons {
  display: flex;
  gap: 6px;
}
</style>
