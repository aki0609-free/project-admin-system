<script setup lang="ts" generic="Schema extends ZodObject<any>">
import { computed, ref } from 'vue'
import FormProvider from './FormProvider.vue'
import type { ZodObject, z } from 'zod'

type T = z.infer<Schema>

const props = defineProps<{
  modelValue: T
  schema: Schema
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: T): void
}>()

const model = computed<T>({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})

const formProviderRef = ref<{ validateAll: () => boolean } | null>()
defineExpose({
  validateAll: () => formProviderRef.value?.validateAll() ?? false,
})
</script>

<template>
  <FormProvider ref="formProviderRef" v-model="model" :schema="schema">
    <v-card class="form-layout-card pa-6 rounded-lg" elevation="1" variant="outlined">
      <div class="form-layout-content">
        <slot />
      </div>
    </v-card>
  </FormProvider>
</template>

<style scoped>
.form-layout-card {
  border: 1px solid rgba(var(--v-theme-on-surface), 0.2);
  background-color: white;
}

.form-layout-content {
  display: grid;
  align-content: start;
  gap: 20px;
  min-width: 0;
}

.form-layout-content > :deep(*) {
  min-width: 0;
}
</style>
