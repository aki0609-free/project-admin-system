<script setup lang="ts">
import { ToolbarItem } from '@/shared/components/toolbar/types/types'
import { PropType } from 'vue'

const props = defineProps({
  items: {
    type: Array as PropType<ToolbarItem[]>,
    required: true,
  },
})
</script>

<template>
  <v-toolbar
    flat
    density="comfortable"
    class="custom-toolbar px-4"
  >
    <div class="d-flex align-center ga-2 w-100">
      <template v-for="(item, i) in items" :key="i">
        <!-- Button -->
        <v-btn
          v-if="item.type === 'button'"
          :color="item.color ?? 'primary'"
          variant="tonal"
          rounded="lg"
          :disabled="item.disabled ?? false"
          @click="item.onClick"
        >
          {{ item.label }}
        </v-btn>

        <!-- Icon -->
        <v-btn
          v-else-if="item.type === 'icon'"
          icon
          variant="text"
          class="icon-btn"
          @click="item.onClick"
        >
          <v-icon>{{ item.icon }}</v-icon>
        </v-btn>

        <!-- Dropdown -->
        <v-menu v-else-if="item.type === 'dropdown'">
          <template #activator="{ props }">
            <v-btn
              v-bind="props"
              variant="tonal"
              rounded="lg"
            >
              {{ item.label }}
            </v-btn>
          </template>

          <v-list density="compact">
            <v-list-item
              v-for="opt in item.options"
              :key="opt.value"
              @click="item.onSelect(opt.value)"
            >
              {{ opt.label }}
            </v-list-item>
          </v-list>
        </v-menu>

        <!-- Search -->
        <v-text-field
          v-else-if="item.type === 'search'"
          :placeholder="item.placeholder ?? 'Search...'"
          :model-value="item.modelValue"
          @update:modelValue="item['onUpdate:modelValue']"
          density="compact"
          hide-details
          variant="solo-filled"
          rounded="lg"
          class="search-field"
        />
      </template>
    </div>
  </v-toolbar>
</template>

<style scoped>
.custom-toolbar {
  background: #f8f9fb;
  border-bottom: 1px solid #e0e0e0;
}

.icon-btn {
  min-width: 40px;
}

.search-field {
  max-width: 240px;
}
</style>