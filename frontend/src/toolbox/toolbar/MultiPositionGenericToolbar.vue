<script setup lang="ts">
import AppToolbar from '@/shared/ui/toolbar/AppToolbar.vue'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'

withDefaults(
  defineProps<{
    leftItems?: ToolbarItem[]
    rightItems?: ToolbarItem[]
  }>(),
  {
    leftItems: () => [],
    rightItems: () => [],
  },
)
</script>

<template>
  <AppToolbar :left-items="leftItems" :right-items="rightItems" surface="plain" />
</template>
