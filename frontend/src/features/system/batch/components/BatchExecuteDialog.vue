<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import type { BatchJobDefinitionResponse } from '@/features/system/batch/types/batchApiTypes'
import DetailDialogLayout from '@/toolbox/dialog/DetailDialogLayout.vue';

const props = defineProps<{
  modelValue: boolean
  definition: BatchJobDefinitionResponse | null
  loading?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'execute', value: { jobCode: string; params: Record<string, unknown> }): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})

const paramsJson = ref('{}')
const errorMessage = ref('')

watch(
  () => props.definition,
  () => {
    paramsJson.value = '{}'
    errorMessage.value = ''
  },
)

const close = () => {
  visible.value = false
}

const execute = () => {
  if (!props.definition) return

  try {
    const parsed = JSON.parse(paramsJson.value || '{}')

    if (
      parsed === null ||
      Array.isArray(parsed) ||
      typeof parsed !== 'object'
    ) {
      errorMessage.value = 'JSONはオブジェクト形式で入力してください。'
      return
    }

    errorMessage.value = ''

    emit('execute', {
      jobCode: props.definition.jobCode,
      params: parsed,
    })
  } catch {
    errorMessage.value = 'JSON形式が正しくありません。'
  }
}

const rightFooterItems = computed<ToolbarItem[]>(() => [
  {
    type: 'button',
    label: '閉じる',
    color: 'secondary',
    disabled: props.loading,
    onClick: close,
  },
  {
    type: 'button',
    label: '実行',
    color: 'primary',
    disabled: !props.definition || props.loading,
    loading: props.loading,
    onClick: execute,
  },
])
</script>

<template>
  <DetailDialogLayout
    v-model="visible"
    title="バッチ実行"
    max-width="760"
    :right-footer-items="rightFooterItems"
  >
    <div
      v-if="definition"
      class="summary"
    >
      <div><strong>jobCode：</strong>{{ definition.jobCode }}</div>
      <div><strong>jobName：</strong>{{ definition.jobName }}</div>
      <div><strong>jobType：</strong>{{ definition.jobType }}</div>
      <div><strong>targetCode：</strong>{{ definition.targetCode }}</div>
    </div>

    <v-textarea
      v-model="paramsJson"
      label="Params JSON"
      variant="outlined"
      rows="12"
      auto-grow
      spellcheck="false"
      class="json-area"
      :error="!!errorMessage"
      :error-messages="errorMessage"
    />
  </DetailDialogLayout>
</template>

<style scoped>
.summary {
  display: grid;
  gap: 4px;
  padding: 12px;
  margin-bottom: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #f8fafc;
}

.json-area :deep(textarea) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}
</style>