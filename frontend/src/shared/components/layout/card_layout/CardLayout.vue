<script setup lang="ts">
import { PropType } from 'vue'

const props = defineProps({
  title: {
    type: String,
    default: '',
  },
  subtitle: {
    type: String,
    default: '',
  },
  elevation: {
    type: Number,
    default: 2,
  },
  loading: {
    type: Boolean,
    default: false,
  },
})
</script>

<template>
  <v-card
    :elevation="elevation"
    rounded="xl"
    class="card-layout"
  >
    <!-- Header -->
    <v-card-item v-if="title || $slots.header">
      <template v-if="$slots.header">
        <slot name="header" />
      </template>

      <template v-else>
        <v-card-title class="text-h6">
          {{ title }}
        </v-card-title>

        <v-card-subtitle v-if="subtitle">
          {{ subtitle }}
        </v-card-subtitle>
      </template>
    </v-card-item>

    <v-divider v-if="title || $slots.header" />

    <!-- Body -->
    <v-card-text class="card-body">
      <v-overlay
        :model-value="loading"
        contained
        class="d-flex align-center justify-center"
      >
        <v-progress-circular indeterminate />
      </v-overlay>

      <slot />
    </v-card-text>

    <!-- Actions -->
    <v-divider v-if="$slots.actions" />

    <v-card-actions v-if="$slots.actions">
      <slot name="actions" />
    </v-card-actions>
  </v-card>
</template>

<style scoped>
.card-layout {
  background: white;
  transition: box-shadow 0.2s ease;
  width: 100%;
}

.card-layout:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.card-body {
  position: relative;
  min-height: 80px;
}
</style>