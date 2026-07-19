<script setup lang="ts" generic="T">
type Item = {
  title: string
  value: T
}

const props = defineProps<{
  modelValue: T[]
  items: Item[]
  label?: string
  getColor?: (val: T) => string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: T[]): void
}>()

const color = (val: T) => {
  return props.getColor?.(val) ?? 'grey'
}
</script>

<template>
  <div>
    <!-- Select -->
    <v-select
      :label="label"
      :items="items"
      :model-value="modelValue"
      multiple
      chips
      @update:modelValue="emit('update:modelValue', $event)"
    />

    <!-- Chips -->
    <div style="margin-top: 8px; display: flex; gap: 4px; flex-wrap: wrap">
      <v-chip
        v-for="val in modelValue"
        :key="String(val)"
        :color="color(val)"
        variant="outlined"
        size="small"
      >
        {{
          items.find(i => i.value === val)?.title ?? val
        }}
      </v-chip>
    </div>
  </div>
</template>