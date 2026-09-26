<script setup lang="ts">
import { computed, nextTick, ref, toRaw, watch } from 'vue'
import {
  SpreadsheetComponent as EjsSpreadsheet,
  type SpreadsheetComponent,
} from '@syncfusion/ej2-vue-spreadsheet'
import { configureSyncfusion } from '@/app/plugins/syncfusion'
import { useSaveGeneratedSpreadsheetLedgerMutation } from '../api/useSaveGeneratedSpreadsheetLedgerMutation'
import { useExportSpreadsheetLedgerXlsxMutation } from '../api/useExportSpreadsheetLedgerXlsxMutation'
import type { SpreadsheetLedgerGenerateResponse } from '../types/operationBookTypes'
import type { SpreadsheetJsonResult } from '@/features/system/excelbook/types/excelBookTypes'
import { formatYearMonth } from '@/shared/utils/DateUtils'
import { prepareSpreadsheetWorkbook } from '../utils/spreadsheetWorkbookPresentation'
import { printSpreadsheetRange } from '../utils/printSpreadsheetRange'
import { downloadBlob } from '@/shared/utils/BusinessUtils'

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
  result: SpreadsheetLedgerGenerateResponse | null
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})

const spreadsheet = ref<SpreadsheetComponent | null>(null)
const ready = ref(false)
const browserLoadDurationMs = ref<number | null>(null)
const applyingWorkbook = ref(false)
const workbookOpenStartedAt = ref<number | null>(null)
const dirty = ref(false)
const saveMessage = ref('')
const saveError = ref(false)
const saveMutation = useSaveGeneratedSpreadsheetLedgerMutation()
const exportMutation = useExportSpreadsheetLedgerXlsxMutation()
const preparingExport = ref(false)
const saving = computed(() => saveMutation.isPending.value)
const exporting = computed(
  () => preparingExport.value || exportMutation.isPending.value,
)
const scrollSettings = computed(() => ({
  enableVirtualization: true,
  isFinite: props.result?.bookCode === 'MONTHLY_SUMMARY',
}))
const sheetCount = computed(() => {
  const workbook = props.result?.workbook as {
    Workbook?: { sheets?: unknown[] }
    sheets?: unknown[]
  } | undefined
  return workbook?.Workbook?.sheets?.length
    ?? workbook?.sheets?.length
    ?? 0
})

configureSyncfusion()

function spreadsheetInstance() {
  return spreadsheet.value?.ej2Instances ?? null
}

function withProjectAdminMetadata(workbook: Record<string, unknown>) {
  const result = structuredClone(workbook)
  const metadata = props.result?.workbook.projectAdminMetadata
  if (metadata && typeof metadata === 'object') {
    // props 配下の値は Vue Proxy のため structuredClone できない。
    // Workbook 本体と同様に、API 送信可能なプレーン値へ変換する。
    result.projectAdminMetadata = JSON.parse(JSON.stringify(metadata))
  }
  return result
}

function currentWorkbookSnapshot() {
  const instance = spreadsheetInstance()
  if (!instance || !props.result) {
    throw new Error('Spreadsheetが初期化されていません。')
  }

  // APIレスポンスはVueのリアクティブProxyになっているため、
  // structuredCloneへ直接渡すとDataCloneErrorになる。JSONダウンロードと
  // 同じ方法で、バックエンドへ送信可能なプレーンオブジェクトへ戻す。
  const workbook = JSON.parse(JSON.stringify(props.result.workbook)) as {
    Workbook?: { sheets?: unknown[]; activeSheetIndex?: number }
    sheets?: unknown[]
    activeSheetIndex?: number
  }
  // Syncfusion の Sheet インスタンスには循環参照が含まれるため、そのまま
  // JSON化できない。Excel出力・部分印刷は、直近に生成（または保存）された
  // Workbookを使い、現在表示中のシート位置だけを反映する。
  if (workbook.Workbook) {
    workbook.Workbook.activeSheetIndex = instance.activeSheetIndex
  } else {
    workbook.activeSheetIndex = instance.activeSheetIndex
  }
  return withProjectAdminMetadata(workbook as Record<string, unknown>)
}

async function openWorkbook() {
  if (!ready.value || !props.result) return

  const instance = spreadsheetInstance()
  if (!instance) return

  applyingWorkbook.value = true
  workbookOpenStartedAt.value = performance.now()
  instance.openFromJson({
    file: prepareSpreadsheetWorkbook(toRaw(props.result.workbook)),
    triggerEvent: true,
  })
}

async function handleOpenComplete() {
  await nextTick()
  await nextAnimationFrame()
  await nextAnimationFrame()
  applyMonthlyLaborHeaderOverflow()
  if (workbookOpenStartedAt.value !== null) {
    browserLoadDurationMs.value = Math.round(
      performance.now() - workbookOpenStartedAt.value,
    )
  }
  dirty.value = false
  applyingWorkbook.value = false
}

function applyMonthlyLaborHeaderOverflow() {
  if (props.result?.bookCode !== 'MONTHLY_LABOR') return

  const instance = spreadsheetInstance()
  if (!instance) return

  for (const rowIndex of [0, 1]) {
    const cell = instance.getCell(rowIndex, 0)
    if (!cell) continue
    cell.style.overflow = 'visible'
    cell.style.position = 'relative'
    cell.style.zIndex = '2'
    cell.style.whiteSpace = 'nowrap'
  }
}

async function handleCreated() {
  ready.value = true
  const instance = spreadsheetInstance()
  if (instance) {
    instance.hideFileMenuItems(
      [`${instance.element.id}_Open`],
      true,
      true,
    )
  }
  await openWorkbook()
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

function downloadJson() {
  if (!props.result) return

  const data = JSON.stringify(props.result.workbook, null, 2)
  const url = URL.createObjectURL(
    new Blob([data], { type: 'application/json' }),
  )
  const link = document.createElement('a')
  link.href = url
  link.download =
    `${props.result.bookCode}-${props.result.targetMonth}.json`
  link.click()
  URL.revokeObjectURL(url)
}

function printWorkbook(type?: 'Workbook' | 'ActiveSheet') {
  const instance = spreadsheetInstance()
  if (!instance || !props.result) return

  const workbook = props.result.workbook as {
    Workbook?: { sheets?: unknown[] }
    sheets?: unknown[]
  }
  const sheets = workbook.Workbook?.sheets ?? workbook.sheets ?? []
  instance.print({
    type: type ?? (sheets.length > 1 ? 'Workbook' : 'ActiveSheet'),
    allowRowColumnHeader: false,
    allowGridLines: false,
  })
}

function printSelectedRange() {
  const instance = spreadsheetInstance()
  if (!instance) return

  saveMessage.value = ''
  saveError.value = false
  try {
    instance.endEdit()
    const sheet = instance.getActiveSheet()
    printSpreadsheetRange(
      currentWorkbookSnapshot(),
      instance.activeSheetIndex,
      sheet.selectedRange ?? 'A1:A1',
    )
  } catch {
    saveError.value = true
    saveMessage.value = '選択範囲の印刷に失敗しました。'
  }
}

async function exportWorkbook() {
  if (!props.result) return
  const instance = spreadsheetInstance()
  if (!instance) return

  saveMessage.value = ''
  saveError.value = false
  preparingExport.value = true
  try {
    instance.endEdit()
    const blob = await exportMutation.mutateAsync({
      bookCode: props.result.bookCode,
      request: { workbook: currentWorkbookSnapshot() },
    })
    downloadBlob(
      blob,
      `${props.result.bookCode}-${props.result.targetMonth}.xlsx`,
    )
  } catch (error) {
    console.error('Failed to export spreadsheet ledger as Excel.', error)
    saveError.value = true
    saveMessage.value = 'Excelファイルの作成に失敗しました。'
  } finally {
    preparingExport.value = false
  }
}

function handleSpreadsheetChange() {
  if (!applyingWorkbook.value && props.result?.editable) {
    dirty.value = true
    saveMessage.value = ''
  }
}

async function saveWorkbook() {
  if (!props.result?.editable) return

  const instance = spreadsheetInstance()
  if (!instance) return

  saveMessage.value = ''
  saveError.value = false
  try {
    instance.endEdit()
    const json = await instance.saveAsJson() as SpreadsheetJsonResult
    await saveMutation.mutateAsync({
      bookCode: props.result.bookCode,
      targetMonth: props.result.targetMonth,
      selectionValue: props.result.selectionValue,
      request: { workbook: json.jsonObject },
    })
    dirty.value = false
    saveMessage.value = '生成台帳の変更を保存しました。'
  } catch {
    saveError.value = true
    saveMessage.value = '生成台帳の保存に失敗しました。'
  }
}

function nextAnimationFrame() {
  return new Promise<void>(resolve => {
    requestAnimationFrame(() => resolve())
  })
}

function formatBytes(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`
}

watch(
  () => props.result,
  async () => {
    if (visible.value) {
      await openWorkbook()
    }
  },
)

watch(visible, value => {
  if (!value) {
    ready.value = false
    workbookOpenStartedAt.value = null
    browserLoadDurationMs.value = null
    dirty.value = false
    saveMessage.value = ''
  }
})
</script>

<template>
  <v-dialog
    v-model="visible"
    fullscreen
    transition="dialog-bottom-transition"
  >
    <v-card class="generated-ledger-dialog">
      <v-toolbar color="primary">
        <v-btn
          icon="mdi-close"
          aria-label="閉じる"
          @click="handleClose"
        />
        <v-toolbar-title>
          生成台帳：{{ result?.bookName }}
        </v-toolbar-title>
        <v-spacer />
        <v-chip
          v-if="dirty"
          class="mr-3"
          color="warning"
          variant="flat"
        >
          未保存
        </v-chip>
        <v-btn
          v-if="result?.editable"
          prepend-icon="mdi-content-save"
          variant="elevated"
          color="white"
          class="mr-2"
          :loading="saving"
          :disabled="!dirty"
          @click="saveWorkbook"
        >
          変更を保存
        </v-btn>
        <v-menu>
          <template #activator="{ props: activatorProps }">
            <v-btn
              v-bind="activatorProps"
              prepend-icon="mdi-printer"
              append-icon="mdi-menu-down"
              variant="elevated"
              color="white"
              class="mr-2"
              :disabled="!result"
            >
              印刷
            </v-btn>
          </template>
          <v-list density="compact">
            <v-list-item
              v-if="sheetCount > 1"
              title="全シートを印刷"
              prepend-icon="mdi-book-open-page-variant"
              @click="printWorkbook('Workbook')"
            />
            <v-list-item
              title="現在のシートを印刷"
              prepend-icon="mdi-file-outline"
              @click="printWorkbook('ActiveSheet')"
            />
            <v-list-item
              title="選択範囲を印刷"
              prepend-icon="mdi-selection-drag"
              @click="printSelectedRange"
            />
          </v-list>
        </v-menu>
        <v-btn
          prepend-icon="mdi-microsoft-excel"
          variant="elevated"
          color="white"
          class="mr-2"
          :loading="exporting"
          :disabled="!result"
          @click="exportWorkbook"
        >
          Excelダウンロード
        </v-btn>
        <v-btn
          prepend-icon="mdi-code-json"
          variant="elevated"
          color="white"
          :disabled="!result"
          @click="downloadJson"
        >
          JSONダウンロード
        </v-btn>
        <v-btn class="ml-2 mr-3" variant="text" @click="handleClose">
          閉じる
        </v-btn>
      </v-toolbar>

      <div v-if="result" class="generated-ledger-dialog__meta px-4 py-2">
        <span>対象月: {{ formatYearMonth(result.targetMonth) }}</span>
        <span>データ件数: {{ result.rowCount }}件</span>
        <span>JSONサイズ: {{ formatBytes(result.workbookBytes) }}</span>
        <span>生成時間: {{ result.generationDurationMs }} ms</span>
        <span v-if="browserLoadDurationMs !== null">
          ブラウザ表示: {{ browserLoadDurationMs }} ms
        </span>
        <span>保存先: {{ result.storagePath }}</span>
        <span>{{ result.editable ? '締め前・編集可' : '参照専用' }}</span>
      </div>

      <v-alert
        v-if="saveMessage"
        :type="saveError ? 'error' : 'success'"
        variant="tonal"
        density="compact"
        class="mx-4 mb-2"
      >
        {{ saveMessage }}
      </v-alert>

      <div class="generated-ledger-dialog__editor">
        <EjsSpreadsheet
          v-if="visible && result"
          ref="spreadsheet"
          height="100%"
          locale="ja"
          :allow-editing="result.editable"
          :allow-insert="false"
          :allow-delete="false"
          :allow-open="true"
          :allow-save="false"
          :scroll-settings="scrollSettings"
          :show-aggregate="false"
          :created="handleCreated"
          :open-complete="handleOpenComplete"
          :cell-save="handleSpreadsheetChange"
          :action-complete="handleSpreadsheetChange"
        />
      </div>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.generated-ledger-dialog {
  height: 100vh;
}

.generated-ledger-dialog__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 24px;
  color: rgb(var(--v-theme-on-surface-variant));
  font-size: 0.75rem;
}

.generated-ledger-dialog__editor {
  flex: 1;
  min-height: 0;
  padding: 0 16px 16px;
}

.generated-ledger-dialog__editor :deep(.e-spreadsheet) {
  height: 100% !important;
  border-radius: 4px;
}
</style>
