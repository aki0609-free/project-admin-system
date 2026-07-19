<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    modelValue?: string | null
    label?: string
    hint?: string
    rows?: number
    required?: boolean
  }>(),
  {
    label: '',
    hint: '',
    rows: 12,
    required: false,
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()
</script>

<template>
  <div class="sql-editor-field">
    <label
      v-if="props.label"
      class="sql-editor-label"
    >
      {{ props.label }}

      <span
        v-if="props.required"
        class="required-mark"
      >
        *
      </span>
    </label>

    <textarea
      :value="props.modelValue ?? ''"
      :rows="props.rows"
      class="sql-editor"
      spellcheck="false"
      @input="
        emit(
          'update:modelValue',
          ($event.target as HTMLTextAreaElement).value,
        )
      "
    />

    <div
      v-if="props.hint"
      class="sql-editor-hint"
    >
      {{ props.hint }}
    </div>
  </div>
</template>

<style scoped>
.sql-editor-field {
  display: grid;
  gap: 6px;
  width: 100%;
}

.sql-editor-label {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
}

.required-mark {
  color: #dc2626;
  margin-left: 2px;
}

.sql-editor {
  width: 100%;
  box-sizing: border-box;
  padding: 14px 16px;
  resize: vertical;
  min-height: 220px;

  font-family:
    SFMono-Regular,
    Consolas,
    Monaco,
    monospace;
  font-size: 13px;
  line-height: 1.6;

  color: #e2e8f0;
  background: #0f172a;
  border: 1px solid #334155;
  border-radius: 8px;
  outline: none;
}

.sql-editor:focus {
  border-color: #60a5fa;
}

.sql-editor-hint {
  font-size: 12px;
  color: #64748b;
}
</style>