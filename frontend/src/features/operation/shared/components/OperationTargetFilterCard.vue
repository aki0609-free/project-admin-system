<template>
  <v-card
    variant="outlined"
    class="operation-target-filter-card"
  >
    <v-card-text class="operation-target-filter-card__content">
      <slot />
    </v-card-text>
  </v-card>
</template>

<style scoped>
.operation-target-filter-card {
  width: 100%;
  border-color: #e2e8f0;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: none;
}

.operation-target-filter-card__content {
  padding: 16px;
}
</style>
