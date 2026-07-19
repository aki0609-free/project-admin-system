<script setup lang="ts" generic="Schema extends ZodObject<any>">
import { ref } from 'vue';
import FormProvider from './FormProvider.vue'
import { boolean, ZodObject } from 'zod'
import z from 'zod'

type T = z.infer<Schema>

const props = defineProps<{
  modelValue: T
  schema: Schema
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: T): void
}>()

const formProviderRef = ref<{ validateAll: () => boolean } | null >()
defineExpose({
  validateAll: () => formProviderRef.value?.validateAll() ?? false,
})
</script>

<template>
  <FormProvider v-model="props.modelValue" :schema="schema">
    <v-card
      class="pa-6 rounded-lg"
      elevation="1"
      variant="outlined"
      style="border: 1px solid rgba(var(--v-theme-on-surface), 0.2); background-color: white;"
    >
      <slot />
    </v-card>
  </FormProvider>
</template>
