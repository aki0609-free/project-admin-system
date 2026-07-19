<script setup lang="ts">
import { GridItem } from '@/shared/components/layout/grid_layout/types/types';
import { PropType } from 'vue'

const props = defineProps({
  items: {
    type: Array as PropType<GridItem[]>,
    required: true,
  },
  gap: {
    type: Number,
    default: 4,
  },
})
</script>

<template>
  <v-container fluid class="pa-0">
    <v-row :gutter="gap">
      <template v-for="(item, index) in items" :key="index">
        <v-col
          :cols="item.cols ?? 12"
          :sm="item.sm"
          :md="item.md"
          :lg="item.lg"
          :xl="item.xl"
        >
          <slot :name="`item-${index}`" />
        </v-col>
      </template>
    </v-row>
  </v-container>
</template>