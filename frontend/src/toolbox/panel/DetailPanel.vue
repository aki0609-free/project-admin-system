<script setup lang="ts">
defineProps<{
  title: string
  subtitle?: string
  chip?: string
  emptyMessage?: string
  hasData: boolean
}>()
</script>

<template>
  <div class="detail-panel">
    <template v-if="hasData">

      <div class="detail-header">
        <div>
          <p
            v-if="subtitle"
            class="detail-eyebrow"
          >
            {{ subtitle }}
          </p>

          <h5 class="detail-title">
            {{ title }}
          </h5>
        </div>

        <v-chip
          v-if="chip"
          size="small"
          variant="tonal"
          color="primary"
        >
          {{ chip }}
        </v-chip>
      </div>

      <div class="detail-body">
        <slot />
      </div>

    </template>

    <div v-else class="empty">
      {{ emptyMessage }}
    </div>
  </div>
</template>