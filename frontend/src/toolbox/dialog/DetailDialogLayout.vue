<script setup lang="ts">
import { computed } from 'vue'
import type { ToolbarItem } from '../toolbar/types/types';
import MultiPositionGenericToolbar from '../toolbar/MultiPositionGenericToolbar.vue';

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
  set: value => emit('update:modelValue', value),
})

const resolvedLeftFooterItems = computed(() => {
  if (props.leftFooterItems.length > 0) {
    return props.leftFooterItems
  }

  return props.footerItems
})

const resolvedRightFooterItems = computed(() => props.rightFooterItems)

const hasFooter = computed(
  () =>
    resolvedLeftFooterItems.value.length > 0 ||
    resolvedRightFooterItems.value.length > 0,
)
</script>

<template>
  <v-dialog
    v-model="visible"
    :max-width="maxWidth"
    :scrollable="scrollable"
    :persistent="persistent"
  >
    <v-card class="detail-dialog-card">
      <v-card-title class="detail-dialog-title">
        <slot name="title">
          {{ title }}
        </slot>
      </v-card-title>

      <v-divider />

      <v-card-text class="detail-dialog-body">
        <slot />
      </v-card-text>

      <v-divider v-if="hasFooter || $slots.footer" />

      <div
        v-if="hasFooter || $slots.footer"
        class="detail-dialog-footer"
      >
        <slot name="footer">
          <MultiPositionGenericToolbar
            :left-items="resolvedLeftFooterItems"
            :right-items="resolvedRightFooterItems"
          />
        </slot>
      </div>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.detail-dialog-card {
  width: 100%;
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.detail-dialog-title {
  padding: 16px 20px;
  font-weight: 700;
  flex-shrink: 0;
}

.detail-dialog-body {
  display: block;
  padding: 16px 20px;
  overflow-y: auto;
  overflow-x: hidden;
  flex: 1 1 auto;
  min-height: 0;
  max-height: calc(100vh - 170px);
}

.detail-dialog-footer {
  flex-shrink: 0;
  background: #ffffff;
  padding: 8px 12px;
}
</style>