<script setup lang="ts">
const props = defineProps<{
  title?: string
  message?: string
  retry?: () => void
}>()

const displayTitle = props.title ?? 'エラーが発生しました'
const displayMessage = props.message ?? 'しばらくしてからもう一度お試しください'
</script>

<template>
  <div class="error-wrapper">
    <v-icon color="error" size="64">mdi-alert-circle</v-icon>
    <h2>{{ displayTitle }}</h2>
    <p>{{ displayMessage }}</p>

    <v-btn
      v-if="props.retry"
      color="primary"
      variant="outlined"
      @click="props.retry"
    >
      再試行
    </v-btn>
  </div>
</template>

<style scoped>
.error-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 32px;
  gap: 16px;
}

.error-wrapper h2 {
  font-size: 1.5rem;
  margin: 0;
}

.error-wrapper p {
  color: #777;
  margin: 0;
}
</style>