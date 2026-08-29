<script setup lang="ts">
import { computed, ref } from 'vue'
import type { PropType } from 'vue'
import AppToolbar from './AppToolbar.vue'
import BatchToolbarExecutionDialog from './BatchToolbarExecutionDialog.vue'
import type { ToolbarItem } from './types'

const props = defineProps({
  leftItems: {
    type: Array as PropType<ToolbarItem[]>,
    default: () => [],
  },
  rightItems: {
    type: Array as PropType<ToolbarItem[]>,
    default: () => [],
  },
})

const batchDialogVisible = ref(false)
const selectedBatchItem = ref<Extract<ToolbarItem, { type: 'batch' }> | null>(null)

const hasBatchItems = computed(
  () => [...props.leftItems, ...props.rightItems]
    .some(item => item.type === 'batch'),
)

const openBatchDialog = (item: Extract<ToolbarItem, { type: 'batch' }>) => {
  selectedBatchItem.value = item
  batchDialogVisible.value = true
}
</script>

<template>
  <AppToolbar
    :left-items="props.leftItems"
    :right-items="props.rightItems"
    @batch-click="openBatchDialog"
  />

  <BatchToolbarExecutionDialog
    v-if="hasBatchItems"
    v-model="batchDialogVisible"
    :item="selectedBatchItem"
  />
</template>
