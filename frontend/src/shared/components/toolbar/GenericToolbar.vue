<script setup lang="ts">
import AppToolbar from '@/shared/ui/toolbar/AppToolbar.vue'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'

defineProps<{
  items: ToolbarItem[]
}>()
</script>

<template>
  <AppToolbar :left-items="items" :right-items="[]" surface="plain" />
</template>
