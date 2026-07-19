<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as monaco from 'monaco-editor'
import 'monaco-editor/esm/vs/language/json/monaco.contribution'
import prettier from 'prettier/standalone'
import estreePlugin from 'prettier/plugins/estree'
import babelPlugin from 'prettier/plugins/babel'

const props = withDefaults(
  defineProps<{
    modelValue: string
    language?: string
    height?: string
    readOnly?: boolean
    autoFormatOnMount?: boolean
    saveOnCtrlS?: boolean
  }>(),
  {
    language: 'json',
    height: '320px',
    readOnly: false,
    autoFormatOnMount: false,
    saveOnCtrlS: true,
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'save', value: string): void
  (e: 'validation-change', errors: string[]): void
}>()

const containerRef = ref<HTMLElement | null>(null)

let editor: monaco.editor.IStandaloneCodeEditor | null = null
let model: monaco.editor.ITextModel | null = null
let disposable: monaco.IDisposable | null = null

const prefersDark = window.matchMedia?.('(prefers-color-scheme: dark)')

const theme = ref(prefersDark?.matches ? 'vs-dark' : 'vs')

const normalizedHeight = computed(() => props.height)

const getJsonErrors = () => {
  if (!model) return []

  const markers = monaco.editor.getModelMarkers({
    resource: model.uri,
  })

  return markers.map((marker) => marker.message)
}

const validateNow = () => {
  window.setTimeout(() => {
    emit('validation-change', getJsonErrors())
  }, 50)
}

const format = async () => {
  if (!editor) return

  const value = editor.getValue()

  if (props.language === 'json') {
    try {
      const formatted = await prettier.format(value || '{}', {
        parser: 'json',
        plugins: [babelPlugin, estreePlugin],
      })

      editor.setValue(formatted.trim())
      return
    } catch {
      editor.getAction('editor.action.formatDocument')?.run()
      return
    }
  }

  editor.getAction('editor.action.formatDocument')?.run()
}

const focus = () => {
  editor?.focus()
}

const setTheme = (nextTheme: string) => {
  theme.value = nextTheme
  monaco.editor.setTheme(nextTheme)
}

const handleSystemThemeChange = (event: MediaQueryListEvent) => {
  setTheme(event.matches ? 'vs-dark' : 'vs')
}

onMounted(async () => {
  await nextTick()

  if (!containerRef.value) return

  model = monaco.editor.createModel(props.modelValue, props.language)

  const jsonLanguage = monaco.languages.json as unknown as {
    jsonDefaults: {
      setDiagnosticsOptions: (options: {
        validate: boolean
        allowComments: boolean
        trailingCommas: 'ignore' | 'warning' | 'error'
      }) => void
    }
  }

  jsonLanguage.jsonDefaults.setDiagnosticsOptions({
    validate: true,
    allowComments: false,
    trailingCommas: 'error',
  })

  editor = monaco.editor.create(containerRef.value, {
    model,
    theme: theme.value,
    readOnly: props.readOnly,
    automaticLayout: true,
    minimap: { enabled: false },
    fontSize: 13,
    tabSize: 2,
    scrollBeyondLastLine: false,
    formatOnPaste: true,
    formatOnType: true,
    wordWrap: 'on',
  })

  disposable = editor.onDidChangeModelContent(() => {
    const value = editor?.getValue() ?? ''
    emit('update:modelValue', value)
    validateNow()
  })

  if (props.saveOnCtrlS) {
    editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyS, () => {
      emit('save', editor?.getValue() ?? '')
    })
  }

  if (props.autoFormatOnMount) {
    await format()
  }

  validateNow()

  prefersDark?.addEventListener?.('change', handleSystemThemeChange)
})

watch(
  () => props.modelValue,
  (value) => {
    if (!editor) return

    const current = editor.getValue()
    if (current !== value) {
      editor.setValue(value)
    }
  },
)

watch(
  () => props.readOnly,
  (value) => {
    editor?.updateOptions({
      readOnly: value,
    })
  },
)

onBeforeUnmount(() => {
  prefersDark?.removeEventListener?.('change', handleSystemThemeChange)
  disposable?.dispose()
  editor?.dispose()
  model?.dispose()

  disposable = null
  editor = null
  model = null
})

defineExpose({
  format,
  focus,
  validateNow,
})
</script>

<template>
  <div class="monaco-wrapper">
    <div ref="containerRef" class="monaco-container" :style="{ height: normalizedHeight }" />
  </div>
</template>

<style scoped>
.monaco-wrapper {
  border: 1px solid #94a3b8;
  border-radius: 8px;
  overflow: hidden;
  background: #0f172a;
}

.monaco-container {
  width: 100%;
}
</style>
