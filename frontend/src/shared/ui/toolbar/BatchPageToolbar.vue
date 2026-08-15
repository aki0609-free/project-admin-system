<script setup lang="ts">
import type { PropType } from 'vue'
import LegacyBatchToolbar from '@/toolbox/toolbar/BatchGenericToolbar.vue'
import type { ToolbarItem } from './types'

defineProps({
  leftItems: {
    type: Array as PropType<ToolbarItem[]>,
    default: () => [],
  },
  rightItems: {
    type: Array as PropType<ToolbarItem[]>,
    default: () => [],
  },
})
</script>

<template>
  <LegacyBatchToolbar
    :left-items="leftItems"
    :right-items="rightItems"
  />
</template>
