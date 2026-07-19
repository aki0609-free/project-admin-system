<script setup lang="ts">
import { toRef } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import type { BatchExecutionLogResponse } from '@/features/system/batch/types/batchApiTypes'
import {
  useBatchExecutionLogTableConfig,
} from '@/features/system/batch/composables/useBatchExecutionLogTableConfig'

const props = defineProps<{
  items: BatchExecutionLogResponse[]
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
  </SimpleTable>
</template>

<style scoped>
.empty-text {
  color: #94a3b8;
}
</style>