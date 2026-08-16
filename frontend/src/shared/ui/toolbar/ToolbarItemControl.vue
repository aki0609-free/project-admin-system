<script setup lang="ts">
import { computed } from 'vue'
import type { ToolbarBatchItem, ToolbarItem } from './types'

const props = defineProps<{
  item: ToolbarItem
}>()

const emit = defineEmits<{
  (e: 'batch-click', item: ToolbarBatchItem): void
}>()

const intentColors = {
  primary: 'primary',
  secondary: 'secondary',
  danger: 'error',
  warning: 'warning',
  utility: undefined,
} as const

const color = computed(() => {
  if ('color' in props.item && props.item.color) return props.item.color
  if (props.item.intent) return intentColors[props.item.intent]
  return props.item.type === 'batch' ? 'secondary' : 'primary'
})
</script>

<template>
  <v-btn
    v-if="item.type === 'button'"
    :color="color"
    variant="tonal"
    rounded="lg"
    :disabled="item.disabled ?? false"
    :loading="item.loading ?? false"
    :title="item.tooltip"
    @click="item.onClick"
  >
    {{ item.label }}
  </v-btn>

  <v-btn
    v-else-if="item.type === 'batch'"
    :color="color"
    variant="tonal"
    rounded="lg"
    :disabled="item.disabled ?? false"
    :loading="item.loading ?? false"
    :title="item.tooltip"
    @click="emit('batch-click', item)"
  >
    {{ item.label }}
  </v-btn>

  <v-btn
    v-else-if="item.type === 'icon'"
    icon
    :color="color"
    variant="text"
    class="icon-button"
    :disabled="item.disabled ?? false"
    :loading="item.loading ?? false"
    :title="item.tooltip ?? item.label"
    :aria-label="item.label ?? item.tooltip"
    @click="item.onClick"
  >
    <v-icon>{{ item.icon }}</v-icon>
  </v-btn>

  <v-menu v-else-if="item.type === 'dropdown'">
    <template #activator="{ props: menuProps }">
      <v-btn
        v-bind="menuProps"
        :color="color"
        variant="tonal"
        rounded="lg"
        :disabled="item.disabled ?? false"
        :loading="item.loading ?? false"
        :title="item.tooltip"
      >
        {{ item.label }}
      </v-btn>
    </template>

    <v-list density="compact">
      <v-list-item
        v-for="option in item.options"
        :key="option.value"
        @click="item.onSelect(option.value)"
      >
        {{ option.label }}
      </v-list-item>
    </v-list>
  </v-menu>

  <v-text-field
    v-else
    :placeholder="item.placeholder ?? '検索...'"
    :model-value="item.modelValue"
    density="compact"
    hide-details
    variant="solo-filled"
    rounded="lg"
    class="search-field"
    :disabled="item.disabled ?? false"
    :title="item.tooltip"
    @update:model-value="item['onUpdate:modelValue']"
  />
</template>

<style scoped>
.icon-button {
  min-width: 40px;
}

.search-field {
  width: min(280px, 35vw);
  min-width: 180px;
}
</style>
