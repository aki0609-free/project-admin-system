<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useQueryClient } from '@tanstack/vue-query'
import { standardRemunerationEditorApi } from '@/features/master/deduction/api/standardRemunerationEditorApi'
import { queryKeys } from '@/features/master/deduction/api/queryKeys'
import type {
  StandardRemunerationEditorResponse,
  StandardRemunerationEditorRow,
} from '@/features/master/deduction/types/standardRemunerationEditorTypes'
import {
  createStandardRemunerationCsvTemplate,
  parseStandardRemunerationCsv,
} from '@/features/master/deduction/utils/standardRemunerationCsv'
import { downloadBlob } from '@/shared/utils/BusinessUtils'
import DateFormField from '@/shared/components/form/base/components/form/DateFormField.vue'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: boolean): void }>()
const queryClient = useQueryClient()
const editor = ref<StandardRemunerationEditorResponse | null>(null)
const rows = ref<StandardRemunerationEditorRow[]>([])
const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const changeReason = ref('標準報酬月額の登録・改定')
const fileInput = ref<HTMLInputElement | null>(null)
const periodFilter = ref<'ALL' | 'CURRENT' | 'SCHEDULED' | 'ENDED'>('ALL')

const baseDecisionTypeItems = [
  { title: '資格取得', value: 'QUALIFICATION' },
  { title: '定時決定', value: 'REGULAR_DECISION' },
  { title: '随時改定', value: 'REVISION' },
  { title: '手入力', value: 'MANUAL' },
]
const legacyDecisionTypeLabels: Record<string, string> = {
  LOCAL_FIXTURE: '初期登録',
  LEGACY: '移行データ',
  CSV: 'CSV取込',
  IMPORT: '外部取込',
}

const dialog = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})
const statusLabel = computed(() => editor.value?.status === 'VALIDATED'
  ? '検証済み・確定待ち'
  : '確定値を表示')
const displayedRows = computed(() => sortRows(rows.value).filter(row =>
  periodFilter.value === 'ALL' || applicationStatus(row) === periodFilter.value,
))
const decisionTypeItems = computed(() => {
  const configuredValues = new Set(baseDecisionTypeItems.map(item => item.value))
  const compatibilityItems = [...new Set(rows.value.map(row => row.sourceType).filter(Boolean))]
    .filter(value => !configuredValues.has(value))
    .map(value => ({
      title: legacyDecisionTypeLabels[value] ?? '既存データ',
      value,
    }))
  return [...baseDecisionTypeItems, ...compatibilityItems]
})

watch(() => props.modelValue, opened => { if (opened) void load() })

async function load() {
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    editor.value = await standardRemunerationEditorApi.find()
    rows.value = sortRows(editor.value.rows.map(row => ({ ...row })))
  } catch (error) {
    errorMessage.value = toErrorMessage(error, '標準報酬月額の取得に失敗しました。')
  } finally {
    loading.value = false
  }
}

function addRow() {
  const employee = editor.value?.employees[0]
  if (!employee) return
  rows.value.push({
    currentRecordId: null,
    employeeId: employee.employeeId,
    employeeCode: employee.employeeCode,
    employeeName: employee.employeeName,
    effectiveFrom: new Date().toISOString().slice(0, 10),
    effectiveTo: null,
    healthStandardRemuneration: 0,
    pensionStandardRemuneration: 0,
    sourceType: 'MANUAL',
    note: null,
    changed: true,
    closed: false,
  })
}

function employeeChanged(row: StandardRemunerationEditorRow) {
  const employee = editor.value?.employees.find(item => item.employeeId === row.employeeId)
  row.employeeCode = employee?.employeeCode ?? ''
  row.employeeName = employee?.employeeName ?? ''
}

function removeRow(row: StandardRemunerationEditorRow) {
  if (row.currentRecordId != null) return
  const index = rows.value.indexOf(row)
  if (index >= 0) rows.value.splice(index, 1)
}

async function saveDraft() {
  saving.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    editor.value = await standardRemunerationEditorApi.saveDraft({
      rows: rows.value.map(row => ({
        currentRecordId: row.currentRecordId,
        employeeId: row.employeeId,
        effectiveFrom: row.effectiveFrom,
        effectiveTo: row.effectiveTo || null,
        healthStandardRemuneration: Number(row.healthStandardRemuneration),
        pensionStandardRemuneration: Number(row.pensionStandardRemuneration),
        sourceType: row.sourceType || 'MANUAL',
        note: row.note || null,
      })),
    })
    rows.value = sortRows(editor.value.rows.map(row => ({ ...row })))
    successMessage.value = '下書きを保存し、期間・マスター金額の検証が完了しました。'
  } catch (error) {
    errorMessage.value = toErrorMessage(error, '下書きの保存・検証に失敗しました。')
  } finally {
    saving.value = false
  }
}

async function confirmValues() {
  if (!editor.value?.batchId) {
    errorMessage.value = '先に下書きを保存してください。'
    return
  }
  if (!changeReason.value.trim()) {
    errorMessage.value = '変更理由を入力してください。'
    return
  }
  const acknowledgeReclosing = editor.value.hasClosedPeriodChanges
    ? confirm('締め済み期間の変更があります。再締めが必要になることを確認して続けますか？')
    : true
  if (!acknowledgeReclosing || !confirm('標準報酬月額を確定しますか？')) return
  saving.value = true
  errorMessage.value = ''
  try {
    await standardRemunerationEditorApi.confirm(editor.value.batchId, {
      changeReason: changeReason.value.trim(), acknowledgeReclosing,
    })
    await queryClient.invalidateQueries({ queryKey: queryKeys.deductions.all })
    await load()
    successMessage.value = '標準報酬月額を確定しました。'
  } catch (error) {
    errorMessage.value = toErrorMessage(error, '標準報酬月額の確定に失敗しました。')
  } finally {
    saving.value = false
  }
}

function downloadCsv() {
  if (!editor.value) return
  const csv = createStandardRemunerationCsvTemplate({ ...editor.value, rows: rows.value })
  downloadBlob(new Blob([csv], { type: 'text/csv;charset=utf-8' }), '従業員別標準報酬月額.csv')
}

async function importCsv(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file || !editor.value) return
  errorMessage.value = ''
  try {
    rows.value = parseStandardRemunerationCsv(await file.text(), editor.value)
    successMessage.value = 'CSVを画面へ取り込みました。下書き保存・検証を実行してください。'
  } catch (error) {
    errorMessage.value = toErrorMessage(error, 'CSVの取り込みに失敗しました。')
  } finally {
    if (fileInput.value) fileInput.value.value = ''
  }
}

function toErrorMessage(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback
}

function todayIso(): string {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

function applicationStatus(row: StandardRemunerationEditorRow): 'CURRENT' | 'SCHEDULED' | 'ENDED' {
  const today = todayIso()
  if (row.effectiveFrom > today) return 'SCHEDULED'
  if (row.effectiveTo && row.effectiveTo < today) return 'ENDED'
  return 'CURRENT'
}

function applicationStatusLabel(row: StandardRemunerationEditorRow): string {
  switch (applicationStatus(row)) {
    case 'CURRENT': return '現在適用中'
    case 'SCHEDULED': return '適用予定'
    case 'ENDED': return '適用終了'
  }
}

function applicationStatusColor(row: StandardRemunerationEditorRow): string {
  switch (applicationStatus(row)) {
    case 'CURRENT': return 'success'
    case 'SCHEDULED': return 'info'
    case 'ENDED': return 'default'
  }
}

function sortRows(source: StandardRemunerationEditorRow[]): StandardRemunerationEditorRow[] {
  const priority = { CURRENT: 0, SCHEDULED: 1, ENDED: 2 }
  return [...source].sort((left, right) => {
    const statusOrder = priority[applicationStatus(left)] - priority[applicationStatus(right)]
    if (statusOrder !== 0) return statusOrder
    const employeeOrder = left.employeeCode.localeCompare(right.employeeCode, 'ja')
    if (employeeOrder !== 0) return employeeOrder
    return right.effectiveFrom.localeCompare(left.effectiveFrom)
  })
}
</script>

<template>
  <v-dialog v-model="dialog" fullscreen transition="dialog-bottom-transition">
    <v-card class="standard-remuneration-editor">
      <header class="editor-header px-5 py-4 d-flex align-center ga-3">
        <v-btn icon="mdi-close" variant="text" aria-label="閉じる" @click="dialog = false" />
        <div>
          <h1 class="text-h5 font-weight-bold">従業員別標準報酬月額</h1>
          <p class="text-body-2 text-medium-emphasis mb-0">
            適用期間ごとの健康保険・厚生年金用標準報酬月額を履歴管理します。
          </p>
        </div>
        <v-spacer />
        <v-chip color="primary" variant="tonal">{{ statusLabel }}</v-chip>
      </header>

      <v-card-text class="pa-5">
        <v-sheet border rounded="lg" class="pa-4 mb-4">
          <div class="d-flex flex-wrap ga-2 align-center">
            <v-btn prepend-icon="mdi-plus" variant="outlined" @click="addRow">履歴を追加</v-btn>
            <v-btn prepend-icon="mdi-download" variant="outlined" @click="downloadCsv">CSV出力</v-btn>
            <v-btn prepend-icon="mdi-upload" variant="outlined" @click="fileInput?.click()">CSV取込</v-btn>
            <input ref="fileInput" type="file" accept=".csv,text/csv" hidden @change="importCsv">
            <v-select
              v-model="periodFilter"
              label="適用状態"
              :items="[
                { title: 'すべて', value: 'ALL' },
                { title: '現在適用中', value: 'CURRENT' },
                { title: '適用予定', value: 'SCHEDULED' },
                { title: '適用終了', value: 'ENDED' },
              ]"
              density="compact"
              hide-details
              style="max-width: 190px"
            />
            <v-text-field
              v-model="changeReason"
              label="変更理由"
              hide-details
              density="compact"
              style="min-width: 320px; flex: 1"
            />
            <v-btn color="primary" variant="outlined" :loading="saving" @click="saveDraft">
              下書き保存・検証
            </v-btn>
            <v-btn color="primary" :disabled="editor?.status !== 'VALIDATED'" :loading="saving" @click="confirmValues">
              確定
            </v-btn>
          </div>
        </v-sheet>
        <v-alert type="info" variant="tonal" class="mb-3">
          金額は標準報酬月額マスターに登録済みの値だけ使用できます。確定前は給与計算へ反映されません。
        </v-alert>
        <v-alert v-if="editor?.hasClosedPeriodChanges" type="warning" variant="tonal" class="mb-3">
          締め済み期間の変更が含まれます。確定後、対象月の再締めが必要です。
        </v-alert>
        <v-alert v-if="errorMessage" type="error" variant="tonal" class="mb-3">{{ errorMessage }}</v-alert>
        <v-alert v-if="successMessage" type="success" variant="tonal" class="mb-3">{{ successMessage }}</v-alert>
        <v-progress-linear v-if="loading" indeterminate class="mb-3" />

        <div class="editor-grid">
          <table>
            <thead><tr>
              <th>社員</th><th>適用状態</th><th>適用開始日</th><th>適用終了日</th>
              <th class="remuneration-column">健康保険用標準報酬月額</th>
              <th class="remuneration-column">厚生年金用標準報酬月額</th>
              <th>決定区分（算定理由）</th><th>備考</th><th>状態</th><th>操作</th>
            </tr></thead>
            <tbody>
              <tr v-for="(row, index) in displayedRows" :key="`${row.currentRecordId ?? 'new'}-${index}`" :class="{ current: applicationStatus(row) === 'CURRENT', changed: row.changed, closed: row.closed }">
                <td>
                  <v-select
                    v-model="row.employeeId"
                    :items="editor?.employees ?? []"
                    item-title="employeeName"
                    item-value="employeeId"
                    density="compact"
                    hide-details
                    :disabled="row.currentRecordId != null"
                    @update:model-value="employeeChanged(row)"
                  >
                    <template #item="{ props: itemProps, item }">
                      <v-list-item v-bind="itemProps" :subtitle="item.raw.employeeCode" />
                    </template>
                  </v-select>
                </td>
                <td>
                  <v-chip :color="applicationStatusColor(row)" size="small" variant="tonal">
                    {{ applicationStatusLabel(row) }}
                  </v-chip>
                </td>
                <td><DateFormField v-model="row.effectiveFrom" density="compact" hide-details /></td>
                <td><DateFormField v-model="row.effectiveTo" density="compact" clearable hide-details /></td>
                <td class="remuneration-column"><v-number-input v-model="row.healthStandardRemuneration" :min="0" :step="1000" density="compact" hide-details suffix="円" /></td>
                <td class="remuneration-column"><v-number-input v-model="row.pensionStandardRemuneration" :min="0" :step="1000" density="compact" hide-details suffix="円" /></td>
                <td>
                  <v-select
                    v-model="row.sourceType"
                    :items="decisionTypeItems"
                    density="compact"
                    hide-details
                  />
                </td>
                <td><v-text-field v-model="row.note" density="compact" hide-details /></td>
                <td><v-chip size="small" :color="row.closed ? 'error' : row.changed ? 'warning' : 'default'" variant="tonal">
                  {{ row.closed ? '締め済み影響' : row.changed ? '変更あり' : '確定済み' }}
                </v-chip></td>
                <td><v-btn icon="mdi-delete" size="small" variant="text" :disabled="row.currentRecordId != null" @click="removeRow(row)" /></td>
              </tr>
            </tbody>
          </table>
        </div>
      </v-card-text>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.standard-remuneration-editor { background: rgb(var(--v-theme-background)); }
.editor-header { background: rgb(var(--v-theme-surface)); border-bottom: 1px solid rgb(var(--v-theme-outline-variant)); }
.editor-grid { overflow: auto; max-height: calc(100vh - 300px); }
table { width: 100%; min-width: 1900px; border-collapse: collapse; }
th, td { border: 1px solid rgb(var(--v-theme-outline-variant)); padding: 6px; min-width: 150px; }
th { position: sticky; top: 0; z-index: 2; background: rgb(var(--v-theme-primary)); color: rgb(var(--v-theme-on-primary)); white-space: nowrap; }
.remuneration-column { min-width: 260px; white-space: nowrap; }
tr.current td { background: rgb(var(--v-theme-success), .08); }
tr.changed td { background: rgb(var(--v-theme-warning), .08); }
tr.closed td { box-shadow: inset 0 1px rgb(var(--v-theme-error)), inset 0 -1px rgb(var(--v-theme-error)); }
</style>
