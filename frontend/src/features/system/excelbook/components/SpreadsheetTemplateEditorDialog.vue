<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import {
  SheetDirective as ESheet,
  SheetsDirective as ESheets,
  SpreadsheetComponent as EjsSpreadsheet,
  type SpreadsheetComponent,
} from '@syncfusion/ej2-vue-spreadsheet'
import { configureSyncfusion } from '@/app/plugins/syncfusion'
import { useSpreadsheetTemplateQuery } from '../api/useSpreadsheetTemplateQuery'
import { useSaveSpreadsheetTemplateMutation } from '../api/useSaveSpreadsheetTemplateMutation'
import type {
  ExcelBookMasterResponse,
  SpreadsheetJsonResult,
  SpreadsheetWorkbook,
} from '../types/excelBookTypes'

import '@syncfusion/ej2-base/styles/material3.css'
import '@syncfusion/ej2-buttons/styles/material3.css'
import '@syncfusion/ej2-dropdowns/styles/material3.css'
import '@syncfusion/ej2-inputs/styles/material3.css'
import '@syncfusion/ej2-navigations/styles/material3.css'
import '@syncfusion/ej2-popups/styles/material3.css'
import '@syncfusion/ej2-splitbuttons/styles/material3.css'
import '@syncfusion/ej2-grids/styles/material3.css'
import '@syncfusion/ej2-vue-spreadsheet/styles/material3.css'

const props = defineProps<{
  modelValue: boolean
  master: ExcelBookMasterResponse | null
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})

const masterId = computed(() => props.master?.id ?? null)
const queryEnabled = computed(() => visible.value && masterId.value != null)
const spreadsheet = ref<SpreadsheetComponent | null>(null)
const jsonFileInput = ref<HTMLInputElement | null>(null)
const spreadsheetReady = ref(false)
const applyingTemplate = ref(false)
const importedTemplate = ref(false)
const dirty = ref(false)
const message = ref('')
const messageType = ref<'success' | 'error' | 'warning'>('success')
const openUrl =
  import.meta.env.VITE_SYNCFUSION_SPREADSHEET_OPEN_URL?.trim() ?? ''
const excelImportEnabled = computed(() => openUrl.length > 0)

const licenseRegistered = configureSyncfusion()
const templateQuery = useSpreadsheetTemplateQuery(masterId, queryEnabled)
const saveMutation = useSaveSpreadsheetTemplateMutation()

const loading = computed(() => templateQuery.isLoading.value)
const saving = computed(() => saveMutation.isPending.value)
const title = computed(() =>
  props.master
    ? `Spreadsheetテンプレート：${props.master.bookName}`
    : 'Spreadsheetテンプレート',
)

function spreadsheetInstance() {
  return spreadsheet.value?.ej2Instances ?? null
}

async function applyTemplate(workbook: SpreadsheetWorkbook | null) {
  if (!spreadsheetReady.value) return

  const instance = spreadsheetInstance()
  if (!instance) return

  applyingTemplate.value = true

  if (workbook) {
    instance.openFromJson({ file: workbook })
    return
  }

  instance.updateCell(
    {
      value: 'テンプレート変数例',
      style: { fontWeight: 'bold' },
    },
    'A1',
  )
  instance.updateCell({ value: '${name}' }, 'A2')
  instance.updateCell({ value: '計算式例' }, 'B1')
  instance.updateCell({ formula: '=1+1' }, 'B2')

  await nextTick()
  dirty.value = false
  applyingTemplate.value = false
}

async function handleCreated() {
  spreadsheetReady.value = true
  await applyTemplate(templateQuery.template.value?.workbook ?? null)
}

function handleSpreadsheetChange() {
  if (!loading.value && !applyingTemplate.value) {
    dirty.value = true
    message.value = ''
  }
}

function handleOpenComplete() {
  dirty.value = importedTemplate.value
  importedTemplate.value = false
  applyingTemplate.value = false
}

function selectJsonTemplate() {
  jsonFileInput.value?.click()
}

async function importJsonTemplate(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  const instance = spreadsheetInstance()
  if (!instance) return

  try {
    const workbook = JSON.parse(
      await file.text(),
    ) as SpreadsheetWorkbook
    if (
      !workbook
      || typeof workbook !== 'object'
      || !('Workbook' in workbook || 'sheets' in workbook)
    ) {
      throw new Error('Invalid workbook JSON')
    }
    importedTemplate.value = true
    applyingTemplate.value = true
    instance.openFromJson({ file: workbook })
    messageType.value = 'warning'
    message.value =
      'JSONテンプレートを読み込みました。「保存」でS3へ登録してください。'
  } catch {
    importedTemplate.value = false
    applyingTemplate.value = false
    messageType.value = 'error'
    message.value = 'JSONテンプレートの読み込みに失敗しました。'
  }
}

async function handleSave() {
  if (!props.master) return

  const instance = spreadsheetInstance()
  if (!instance) return

  message.value = ''

  try {
    instance.endEdit()
    const result = await instance.saveAsJson() as SpreadsheetJsonResult

    await saveMutation.mutateAsync({
      masterId: props.master.id,
      request: {
        workbook: result.jsonObject,
      },
    })

    dirty.value = false
    messageType.value = 'success'
    message.value = 'Spreadsheetテンプレートを保存しました。'
  } catch {
    messageType.value = 'error'
    message.value = 'Spreadsheetテンプレートの保存に失敗しました。'
  }
}

function handleClose() {
  if (dirty.value) {
    const ok = window.confirm(
      '未保存の変更があります。保存せずに閉じますか？',
    )
    if (!ok) return
  }

  visible.value = false
}

watch(
  () => templateQuery.template.value?.workbook,
  async workbook => {
    if (visible.value && spreadsheetReady.value) {
      await applyTemplate(workbook ?? null)
    }
  },
)

watch(visible, value => {
  if (!value) {
    spreadsheetReady.value = false
    dirty.value = false
    message.value = ''
  }
})
</script>

<template>
  <v-dialog
    v-model="visible"
    fullscreen
    persistent
    transition="dialog-bottom-transition"
  >
    <v-card class="spreadsheet-template-dialog">
      <v-toolbar color="primary">
        <v-btn
          icon="mdi-close"
          aria-label="閉じる"
          :disabled="saving"
          @click="handleClose"
        />
        <v-toolbar-title>{{ title }}</v-toolbar-title>
        <v-spacer />
        <input
          ref="jsonFileInput"
          type="file"
          accept="application/json,.json"
          class="d-none"
          @change="importJsonTemplate"
        >
        <v-btn
          prepend-icon="mdi-file-code-outline"
          variant="tonal"
          class="mr-2"
          :disabled="loading || !spreadsheetReady || saving"
          @click="selectJsonTemplate"
        >
          JSON読込
        </v-btn>
        <v-chip
          v-if="dirty"
          class="mr-3"
          color="warning"
          variant="flat"
        >
          未保存
        </v-chip>
        <v-btn
          prepend-icon="mdi-content-save"
          variant="elevated"
          color="white"
          :loading="saving"
          :disabled="loading || !spreadsheetReady"
          @click="handleSave"
        >
          保存
        </v-btn>
        <v-btn
          class="ml-2 mr-3"
          variant="text"
          :disabled="saving"
          @click="handleClose"
        >
          閉じる
        </v-btn>
      </v-toolbar>

      <v-alert
        v-if="!licenseRegistered"
        type="warning"
        variant="tonal"
        density="compact"
        class="ma-3 mb-0"
      >
        Syncfusionライセンスキーが未設定です。
        VITE_SYNCFUSION_LICENSE_KEYを設定して再起動してください。
      </v-alert>

      <v-alert
        v-if="message"
        :type="messageType"
        variant="tonal"
        density="compact"
        closable
        class="ma-3 mb-0"
        @click:close="message = ''"
      >
        {{ message }}
      </v-alert>

      <v-alert
        v-if="templateQuery.isError.value"
        type="error"
        variant="tonal"
        density="compact"
        class="ma-3 mb-0"
      >
        保存済みテンプレートの読み込みに失敗しました。
      </v-alert>

      <div class="spreadsheet-template-dialog__meta px-4 py-2">
        <span>Book Code: {{ master?.bookCode }}</span>
        <span>
          保存先:
          {{
            templateQuery.template.value?.storagePath
              ?? '保存先を確認中'
          }}
        </span>
        <span v-if="excelImportEnabled">
          Excel取込: ファイル → 開く
        </span>
      </div>

      <div class="spreadsheet-template-dialog__editor">
        <v-progress-linear
          v-if="loading"
          indeterminate
          color="primary"
        />
        <EjsSpreadsheet
          v-if="visible"
          ref="spreadsheet"
          height="100%"
          locale="ja"
          :created="handleCreated"
          :cell-save="handleSpreadsheetChange"
          :action-complete="handleSpreadsheetChange"
          :open-complete="handleOpenComplete"
          :allow-open="excelImportEnabled"
          :open-url="openUrl"
        >
          <ESheets>
            <ESheet name="TEMPLATE" />
          </ESheets>
        </EjsSpreadsheet>
      </div>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.spreadsheet-template-dialog {
  height: 100vh;
}

.spreadsheet-template-dialog__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 24px;
  color: rgb(var(--v-theme-on-surface-variant));
  font-size: 0.75rem;
}

.spreadsheet-template-dialog__editor {
  flex: 1;
  min-height: 0;
  padding: 0 16px 16px;
}

.spreadsheet-template-dialog__editor :deep(.e-spreadsheet) {
  height: 100% !important;
  border-radius: 4px;
}
</style>
