<script setup lang="ts">
const props = defineProps<{
  text?: string
  size?: number
}>()

// デフォルト値
const displayText = props.text ?? '読み込み中…'
const spinnerSize = props.size ?? 48
</script>

<template>
  <div class="loading-wrapper">
    <v-progress-circular
      :size="spinnerSize"
      indeterminate
      color="primary"
    />
    <span class="loading-text">{{ displayText }}</span>
  </div>
</template>

<style scoped>
.loading-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.loading-text {
  margin-top: 12px;
  font-weight: 500;
  color: #555;
}
</style>