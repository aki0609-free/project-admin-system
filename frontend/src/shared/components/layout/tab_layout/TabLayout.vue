<script setup lang="ts">
import { TabItem } from '@/shared/components/layout/tab_layout/types/types';

const props = defineProps<{
  modelValue: string
  tabs: TabItem[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const update = (value: string) => {
  emit('update:modelValue', value)
}
</script>

<template>
  <div class="tab-layout">
    <div class="tab-header">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        :class="['tab-button', { active: tab.value === modelValue }]"
        @click="update(tab.value)"
      >
        {{ tab.label }}
      </button>
    </div>

    <div class="tab-body">
      <slot :active="modelValue" />
    </div>
  </div>
</template>

<style scoped>
.tab-layout {
  background-color: white;
}
.tab-header {
  display: flex;
  border-bottom: 1px solid #e5e7eb;
}

.tab-button {
  padding: 8px 16px;
  background: none;
  border: none;
  cursor: pointer;
  transition: 0.2s;
}

.tab-button.active {
  border-bottom: 2px solid #1976d2;
  font-weight: bold;
}

.tab-body {
  padding: 16px;
}
</style>