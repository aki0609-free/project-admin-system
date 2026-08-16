<script setup lang="ts">
import { computed } from 'vue'
import ToolbarItemControl from './ToolbarItemControl.vue'
import type { ToolbarBatchItem, ToolbarItem } from './types'

const props = withDefaults(
  defineProps<{
    leftItems?: ToolbarItem[]
    rightItems?: ToolbarItem[]
    surface?: 'page' | 'dialog' | 'plain'
  }>(),
  {
    leftItems: () => [],
    rightItems: () => [],
    surface: 'page',
  },
)

const emit = defineEmits<{
  (e: 'batch-click', item: ToolbarBatchItem): void
}>()

const visibleLeftItems = computed(() => props.leftItems.filter((item) => item.visible !== false))
const visibleRightItems = computed(() => props.rightItems.filter((item) => item.visible !== false))
</script>

<template>
  <v-toolbar flat density="comfortable" class="app-toolbar" :class="`surface-${surface}`">
    <div class="toolbar-inner">
      <div class="toolbar-side toolbar-start">
        <ToolbarItemControl
          v-for="(item, index) in visibleLeftItems"
          :key="item.id ?? `left-${index}`"
          :item="item"
          @batch-click="emit('batch-click', $event)"
        />
      </div>

      <div class="toolbar-side toolbar-end">
        <ToolbarItemControl
          v-for="(item, index) in visibleRightItems"
          :key="item.id ?? `right-${index}`"
          :item="item"
          @batch-click="emit('batch-click', $event)"
        />
      </div>
    </div>
  </v-toolbar>
</template>

<style scoped>
.app-toolbar {
  min-height: 52px;
  padding: 4px 16px;
  background: #ffffff;
}

.surface-page {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
}

.surface-dialog {
  min-height: 48px;
  padding: 4px 0;
}

.surface-plain {
  padding-inline: 0;
}

.toolbar-inner {
  width: 100%;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.toolbar-side {
  min-width: 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.toolbar-end {
  justify-content: flex-end;
  margin-left: auto;
}

@media (max-width: 720px) {
  .toolbar-inner {
    align-items: stretch;
    flex-direction: column;
    gap: 8px;
  }

  .toolbar-end {
    justify-content: flex-start;
    margin-left: 0;
  }
}
</style>
