<script setup lang="ts">
defineProps<{
  rowTitle: string
  columnKeys: string[]
  showTotals: boolean
}>()
</script>

<template>
  <thead>
    <tr>
      <th class="header sticky-left">{{ rowTitle }}</th>

      <th 
        v-for="(col, i) in columnKeys"
        :key="col"
        class="header"
      >
        {{ col }}
      </th>

      <th v-if="showTotals" class="header total-header">
        合計
      </th>
    </tr>
  </thead>
</template>