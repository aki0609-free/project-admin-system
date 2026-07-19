<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    modelValue?: string | null
    label?: string
    hint?: string
    placeholder?: string
    rows?: number
    required?: boolean
    readonly?: boolean
    disabled?: boolean
  }>(),
  {
    label: '',
    hint: '',
    placeholder: '',
    rows: 12,
    required: false,
    readonly: false,
    disabled: false,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const onInput = (event: Event) => {
  emit(
    'update:modelValue',
    (event.target as HTMLTextAreaElement).value,
  )
}
</script>

<template>
  <div class="dsl-editor-field">
    <div
      v-if="props.label || props.hint"
      class="dsl-editor-header"
    >
      <label
        v-if="props.label"
        class="dsl-editor-label"
      >
        {{ props.label }}

        <span
          v-if="props.required"
          class="required-mark"
        >
          *
        </span>
      </label>

      <div
        v-if="props.hint"
        class="dsl-editor-hint"
      >
        {{ props.hint }}
      </div>
    </div>

    <textarea
      :value="props.modelValue ?? ''"
      :rows="props.rows"
      :placeholder="props.placeholder"
      :readonly="props.readonly"
      :disabled="props.disabled"
      class="dsl-editor"
      spellcheck="false"
      @input="onInput"
    />
  </div>
</template>

<style scoped>
.dsl-editor-field {
  display: grid;
  gap: 8px;
  width: 100%;
}

.dsl-editor-header {
  display: grid;
  gap: 4px;
}

.dsl-editor-label {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
}

.required-mark {
  color: #dc2626;
  margin-left: 2px;
}

.dsl-editor-hint {
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
}

.dsl-editor {
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
  border-radius: 10px;
  outline: none;
  tab-size: 2;
}

.dsl-editor::placeholder {
  color: #64748b;
}

.dsl-editor:focus {
  border-color: #60a5fa;
  box-shadow: 0 0 0 2px rgba(96, 165, 250, 0.2);
}

.dsl-editor:read-only {
  color: #cbd5e1;
  background: #111827;
}

.dsl-editor:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}
</style>