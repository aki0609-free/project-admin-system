<script setup lang="ts">
import { toRef } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import type { BatchJobDefinitionResponse } from '@/features/system/batch/types/batchApiTypes'
import {
  type BatchDefinitionTableRow,
  useBatchDefinitionTableConfig,
} from '@/features/system/batch/composables/useBatchDefinitionTableConfig'

const props = defineProps<{
  items: BatchJobDefinitionResponse[]
}>()

const emit = defineEmits<{
  (e: 'row-click', row: BatchDefinitionTableRow): void
}>()

const {
  rows,
  columns,
  filterRules,
} = useBatchDefinitionTableConfig(toRef(props, 'items'))
</script>

<template>
  <SimpleTable
    table-key="batch-definition-list"
    item-key="id"
    :items="rows"
    :columns="columns"
    :filter-rules="filterRules"
    enable-row-click
    @row-click="emit('row-click', $event)"
  >
    <template #[`item.jobType`]="{ item }">
      <v-chip
        size="small"
        variant="tonal"
        color="primary"
      >
        {{ item.jobType }}
      </v-chip>
    </template>

    <template #[`item.immediateText`]="{ item }">
      <v-chip
        size="small"
        variant="tonal"
        :color="item.raw.immediateExecutable ? 'success' : 'grey'"
      >
        {{ item.immediateText }}
      </v-chip>
    </template>

    <template #[`item.scheduleText`]="{ item }">
      <v-chip
        size="small"
        variant="tonal"
        :color="item.raw.scheduleEnabled ? 'success' : 'grey'"
      >
        {{ item.scheduleText }}
      </v-chip>
    </template>

    <template #[`item.activeText`]="{ item }">
      <v-chip
        size="small"
        variant="tonal"
        :color="item.raw.activeFlag ? 'success' : 'grey'"
      >
        {{ item.activeText }}
      </v-chip>
    </template>
  </SimpleTable>
</template>