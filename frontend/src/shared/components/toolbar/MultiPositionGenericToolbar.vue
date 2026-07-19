<script setup lang="ts">
import type { PropType } from 'vue'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'

defineProps({
  leftItems: {
    type: Array as PropType<ToolbarItem[]>,
    default: () => [],
  },
  rightItems: {
    type: Array as PropType<ToolbarItem[]>,
    default: () => [],
  },
})
</script>

<template>
  <v-toolbar
    flat
    density="comfortable"
    class="custom-toolbar px-4"
  >
    <div class="toolbar-inner">
      <div class="toolbar-side">
        <template
          v-for="(item, i) in leftItems"
          :key="`left-${i}`"
        >
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

          <v-btn
            v-else-if="item.type === 'icon'"
            icon
            variant="text"
            class="icon-btn"
            :disabled="item.disabled ?? false"
            @click="item.onClick"
          >
            <v-icon>{{ item.icon }}</v-icon>
          </v-btn>

          <v-menu v-else-if="item.type === 'dropdown'">
            <template #activator="{ props }">
              <v-btn
                v-bind="props"
                variant="tonal"
                rounded="lg"
                :disabled="item.disabled ?? false"
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

          <v-text-field
            v-else-if="item.type === 'search'"
            :placeholder="item.placeholder ?? 'Search...'"
            :model-value="item.modelValue"
            density="compact"
            hide-details
            variant="solo-filled"
            rounded="lg"
            class="search-field"
            @update:model-value="item['onUpdate:modelValue']"
          />
        </template>
      </div>

      <div class="toolbar-side">
        <template
          v-for="(item, i) in rightItems"
          :key="`right-${i}`"
        >
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

          <v-btn
            v-else-if="item.type === 'icon'"
            icon
            variant="text"
            class="icon-btn"
            :disabled="item.disabled ?? false"
            @click="item.onClick"
          >
            <v-icon>{{ item.icon }}</v-icon>
          </v-btn>

          <v-menu v-else-if="item.type === 'dropdown'">
            <template #activator="{ props }">
              <v-btn
                v-bind="props"
                variant="tonal"
                rounded="lg"
                :disabled="item.disabled ?? false"
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

          <v-text-field
            v-else-if="item.type === 'search'"
            :placeholder="item.placeholder ?? 'Search...'"
            :model-value="item.modelValue"
            density="compact"
            hide-details
            variant="solo-filled"
            rounded="lg"
            class="search-field"
            @update:model-value="item['onUpdate:modelValue']"
          />
        </template>
      </div>
    </div>
  </v-toolbar>
</template>

<style scoped>
.custom-toolbar {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding-top: 4px;
  padding-bottom: 4px;
}

.toolbar-inner {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.toolbar-side {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-btn {
  min-width: 40px;
}

.search-field {
  max-width: 240px;
}
</style>