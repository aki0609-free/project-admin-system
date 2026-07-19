<script setup lang="ts">
defineProps<{
  totalCount: number
  importantCount: number
  warningCount: number
  pinnedCount: number
}>()
</script>

<template>
  <div class="summary-area">
    <v-chip color="primary" variant="tonal">
      全 {{ totalCount }} 件
    </v-chip>

    <v-chip color="red" variant="tonal">
      重要 {{ importantCount }} 件
    </v-chip>

    <v-chip color="orange" variant="tonal">
      警告 {{ warningCount }} 件
    </v-chip>

    <v-chip color="purple" variant="tonal">
      固定 {{ pinnedCount }} 件
    </v-chip>
  </div>
</template>

<style scoped>
.summary-area {
  display: flex;
  gap: 8px;
  padding: 12px 20px;
  flex-wrap: wrap;
}
</style>