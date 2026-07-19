<script setup lang="ts" generic="Schema extends ZodObject<any>">
import FormProvider from './FormProvider.vue'
import { ZodObject } from 'zod'
import z from 'zod'
import NestedFormProvider from './NestedFormProvider.vue';

type T = z.infer<Schema>

const props = defineProps<{
  modelValue: T
  schema: Schema
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: T): void
}>()
</script>

<template>
  <NestedFormProvider
    v-model="props.modelValue"
    :schema="schema"
  >
    <v-card variant="outlined" class="pa-6">
      <slot />
    </v-card>
  </NestedFormProvider>
</template>