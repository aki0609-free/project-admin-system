<script setup lang="ts">
import { computed } from 'vue'
import AppDialog from '@/shared/ui/dialog/AppDialog.vue'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    title: string
    maxWidth?: string | number
    scrollable?: boolean
    persistent?: boolean
    footerItems?: ToolbarItem[]
    leftFooterItems?: ToolbarItem[]
    rightFooterItems?: ToolbarItem[]
  }>(),
  {
    maxWidth: 1280,
    scrollable: true,
    persistent: true,
    footerItems: () => [],
    leftFooterItems: () => [],
    rightFooterItems: () => [],
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const resolvedLeftFooterItems = computed(() =>
  props.leftFooterItems.length > 0 ? props.leftFooterItems : props.footerItems,
)
</script>

<template>
  <AppDialog
    v-model="visible"
    :title="title"
    :max-width="maxWidth"
    :scrollable="scrollable"
    :persistent="persistent"
    :left-footer-items="resolvedLeftFooterItems"
    :right-footer-items="rightFooterItems"
  >
    <template v-if="$slots.title" #title>
      <slot name="title" />
    </template>

    <slot />

    <template v-if="$slots.footer" #footer>
      <slot name="footer" />
    </template>
  </AppDialog>
</template>
