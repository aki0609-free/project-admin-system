<script setup lang="ts">
import { computed, type PropType } from 'vue'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import BatchGenericToolbar from '../toolbar/BatchGenericToolbar.vue'

const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  description: {
    type: String,
    default: '',
  },
  leftToolbarItems: {
    type: Array as PropType<ToolbarItem[]>,
    default: () => [],
  },
  rightToolbarItems: {
    type: Array as PropType<ToolbarItem[]>,
    default: () => [],
  },
  contentPadding: {
    type: Boolean,
    default: false,
  },
})

const hasToolbar = computed(
  () =>
    props.leftToolbarItems.length > 0 ||
    props.rightToolbarItems.length > 0,
)
</script>

<template>
  <div class="list-detail-page-layout">
    <div class="page-header">
      <div class="page-header-text">
        <h2 class="page-title">
          {{ title }}
        </h2>

        <p
          v-if="description"
          class="page-description"
        >
          {{ description }}
        </p>
      </div>

      <slot name="header-actions" />
    </div>

    <BatchGenericToolbar
      v-if="hasToolbar"
      :left-items="leftToolbarItems"
      :right-items="rightToolbarItems"
    />

    <div v-if="$slots.search" class="page-search">
      <slot name="search" />
    </div>

    <div v-if="$slots['before-table']" class="before-table">
      <slot name="before-table" />
    </div>

    <div
      class="page-content"
      :class="{ 'with-padding': contentPadding }"
    >
      <slot />
    </div>

    <slot name="dialogs" />
  </div>
</template>

<style scoped>
.list-detail-page-layout {
  display: grid;
  gap: 16px;
  width: 100%;
  min-height: 100%;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.page-header-text {
  display: grid;
  gap: 6px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.35;
  color: #0f172a;
}

.page-description {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

.page-search {
  min-width: 0;
}

.before-table {
  min-width: 0;
}

.page-content {
  min-width: 0;
  min-height: 0;
}

.page-content.with-padding {
  padding: 16px;
  border-radius: 12px;
  background: #ffffff;
}
</style>