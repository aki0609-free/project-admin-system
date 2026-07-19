<script setup lang="ts">
const props = defineProps<{
  aggregation: 'sum' | 'avg' | 'count'
  sort: 'none' | 'asc' | 'desc'
}>()

const emit = defineEmits<{
  (e: 'update:aggregation', v: 'sum' | 'avg' | 'count'): void
  (e: 'update:sort', v: 'none' | 'asc' | 'desc'): void
}>()
</script>

<template>
  <div class="pivot-toolbar">
    <slot name="title" />
    <slot name="actions" />

    <div class="toolbar-right">
      <v-btn-toggle
        :model-value="aggregation"
        @update:model-value="emit('update:aggregation', $event)"
        mandatory
      >
        <v-btn value="sum">SUM</v-btn>
        <v-btn value="avg">AVG</v-btn>
        <v-btn value="count">COUNT</v-btn>
      </v-btn-toggle>

      <v-btn-toggle
        :model-value="sort"
        @update:model-value="emit('update:sort', $event)"
      >
        <v-btn value="none">標準</v-btn>
        <v-btn value="desc">↓</v-btn>
        <v-btn value="asc">↑</v-btn>
      </v-btn-toggle>
    </div>
  </div>
</template>