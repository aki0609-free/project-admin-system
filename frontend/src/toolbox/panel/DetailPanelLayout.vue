<script setup lang="ts">
defineProps<{
  title: string
  subtitle?: string
  chip?: string | null

  hasData: boolean

  emptyMessage?: string
}>()
</script>

<template>
  <div class="detail-panel-layout">
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

    <div
      v-else
      class="empty"
    >
      {{ emptyMessage }}
    </div>
  </div>
</template>

<style scoped>
.detail-panel-layout {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fafafa;
  overflow: hidden;
  height: 100%;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px;
  background: #f1f5f9;
  border-bottom: 1px solid #e5e7eb;
}

.detail-body {
  padding: 16px;
}

.detail-eyebrow {
  margin: 0 0 4px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #64748b;
}

.detail-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.empty {
  display: grid;
  place-items: center;
  min-height: 220px;
  color: #64748b;
  text-align: center;
  line-height: 1.8;
  padding: 24px;
}
</style>