<script setup lang="ts">
import { ref } from 'vue'
import MonacoWrapper from '@/shared/components/monaco/MonacoWrapper.vue'

defineProps<{
  modelValue: string
  errorMessage?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'save'): void
}>()

const monacoRef = ref<InstanceType<typeof MonacoWrapper> | null>(null)
const markerErrors = ref<string[]>([])

const format = async () => {
  await monacoRef.value?.format()
}

defineExpose({
  format,
})
</script>

<template>
  <div class="json-editor">
    <MonacoWrapper
      ref="monacoRef"
      :model-value="modelValue"
      language="json"
      height="340px"
      @update:model-value="emit('update:modelValue', $event)"
      @save="emit('save')"
      @validation-change="markerErrors = $event"
    />

    <div
      v-if="errorMessage || markerErrors.length"
      class="error-text"
    >
      {{ errorMessage || markerErrors[0] }}
    </div>
  </div>
</template>

<style scoped>
.json-editor {
  display: grid;
}

.error-text {
  padding: 8px 12px;
  color: #b91c1c;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-top: none;
  font-size: 13px;
}
</style>