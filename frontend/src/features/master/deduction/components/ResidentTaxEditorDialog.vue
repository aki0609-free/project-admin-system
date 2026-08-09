<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useQueryClient } from '@tanstack/vue-query'
import { residentTaxEditorApi } from '@/features/master/deduction/api/residentTaxEditorApi'
import { queryKeys } from '@/features/master/deduction/api/queryKeys'
import type {
  ResidentTaxEditorResponse,
  ResidentTaxEmployeeEditor,
} from '@/features/master/deduction/types/residentTaxEditorTypes'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: boolean): void }>()

const queryClient = useQueryClient()
const currentDate = new Date()
const fiscalYear = ref(currentDate.getMonth() + 1 >= 6
  ? currentDate.getFullYear()
  : currentDate.getFullYear() - 1)
const editor = ref<ResidentTaxEditorResponse | null>(null)
const rows = ref<ResidentTaxEmployeeEditor[]>([])
const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const changeReason = ref('住民税通知に基づく年度税額登録')

const dialog = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})

const monthLabels: Record<number, string> = {
  1: '1月', 2: '2月', 3: '3月', 4: '4月', 5: '5月', 6: '6月',
  7: '7月', 8: '8月', 9: '9月', 10: '10月', 11: '11月', 12: '12月',
}

watch(() => props.modelValue, opened => {
  if (opened) void load()
})

watch(fiscalYear, () => {
  if (props.modelValue) void load()
})

async function load() {
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    editor.value = await residentTaxEditorApi.find(fiscalYear.value)
    rows.value = editor.value.employees.map(employee => ({
      ...employee,
      months: employee.months.map(month => ({ ...month })),
    }))
  } catch (error) {
    errorMessage.value = toErrorMessage(error, '住民税データの取得に失敗しました。')
  } finally {
    loading.value = false
  }
}

function normalizeAmount(value: unknown): number | null {
  if (value === '' || value == null) return null
  const numberValue = Number(value)
  return Number.isFinite(numberValue) ? Math.trunc(numberValue) : null
}

function copyJulyForward(row: ResidentTaxEmployeeEditor) {
  const july = row.months.find(month => month.month === 7)?.draftTaxAmount ?? null
  for (const month of row.months) {
    if (month.month !== 6) month.draftTaxAmount = july
  }
}

function handlePaste(event: ClipboardEvent, rowIndex: number, monthIndex: number) {
  const text = event.clipboardData?.getData('text')
  if (!text || (!text.includes('\t') && !text.includes('\n'))) return
  event.preventDefault()
  const matrix = text.trimEnd().split(/\r?\n/).map(line => line.split('\t'))
  matrix.forEach((columns, rowOffset) => {
    const targetRow = rows.value[rowIndex + rowOffset]
    if (!targetRow) return
    columns.forEach((cell, columnOffset) => {
      const targetMonth = targetRow.months[monthIndex + columnOffset]
      if (!targetMonth) return
      targetMonth.draftTaxAmount = normalizeAmount(cell.replaceAll(',', ''))
    })
  })
}

async function saveDraft() {
  saving.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    editor.value = await residentTaxEditorApi.saveDraft({
      fiscalYear: fiscalYear.value,
      employees: rows.value.map(employee => ({
        employeeId: employee.employeeId,
        months: employee.months.map(month => ({
          month: month.month,
          taxAmount: normalizeAmount(month.draftTaxAmount),
        })),
      })),
    })
    rows.value = editor.value.employees.map(employee => ({
      ...employee,
      months: employee.months.map(month => ({ ...month })),
    }))
    successMessage.value = '下書きを保存し、入力内容の検証が完了しました。'
  } catch (error) {
    errorMessage.value = toErrorMessage(error, '下書きの保存に失敗しました。')
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
  const acknowledgeReclosing = editor.value.hasClosedMonthChanges
    ? confirm('締め済み月の変更が含まれます。確定後に対象月を再締めする必要があります。続けますか？')
    : true
  if (!acknowledgeReclosing) return
  if (!confirm(`${fiscalYear.value}年度の住民税を確定しますか？`)) return

  saving.value = true
  errorMessage.value = ''
  try {
    await residentTaxEditorApi.confirm(editor.value.batchId, {
      changeReason: changeReason.value.trim(),
      acknowledgeReclosing,
    })
    await queryClient.invalidateQueries({ queryKey: queryKeys.deductions.all })
    successMessage.value = '住民税を確定しました。'
    await load()
  } catch (error) {
    errorMessage.value = toErrorMessage(error, '住民税の確定に失敗しました。')
  } finally {
    saving.value = false
  }
}

function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof Error && error.message) return error.message
  if (typeof error === 'object' && error !== null) {
    const candidate = error as Record<string, unknown>
    if (typeof candidate.message === 'string') return candidate.message
  }
  return fallback
}
</script>

<template>
  <v-dialog v-model="dialog" fullscreen transition="dialog-bottom-transition">
    <v-card>
      <v-toolbar color="primary">
        <v-btn icon="mdi-close" @click="dialog = false" />
        <v-toolbar-title>年度別住民税Editor</v-toolbar-title>
        <v-spacer />
        <v-btn :loading="saving" @click="saveDraft">下書き保存・検証</v-btn>
        <v-btn :disabled="editor?.status !== 'VALIDATED'" :loading="saving" @click="confirmValues">
          確定
        </v-btn>
      </v-toolbar>

      <v-card-text class="pa-4">
        <div class="d-flex flex-wrap align-center ga-3 mb-4">
          <v-number-input
            v-model="fiscalYear"
            label="年度（6月～翌年5月）"
            :min="2000"
            :max="2100"
            control-variant="stacked"
            style="max-width: 240px"
          />
          <v-chip>状態: {{ editor?.status ?? 'NONE' }}</v-chip>
          <v-text-field
            v-model="changeReason"
            label="変更理由"
            style="min-width: 360px"
          />
        </div>

        <v-alert v-if="errorMessage" type="error" variant="tonal" class="mb-3">
          {{ errorMessage }}
        </v-alert>
        <v-alert v-if="successMessage" type="success" variant="tonal" class="mb-3">
          {{ successMessage }}
        </v-alert>
        <v-alert v-if="editor?.hasClosedMonthChanges" type="warning" variant="tonal" class="mb-3">
          締め済み月の変更が含まれます。確定後に月次の再締めが必要です。
        </v-alert>

        <v-progress-linear v-if="loading" indeterminate class="mb-3" />

        <div class="resident-tax-grid">
          <table>
            <thead>
              <tr>
                <th class="sticky employee-code">社員コード</th>
                <th class="sticky employee-name">氏名</th>
                <th v-for="month in rows[0]?.months ?? []" :key="month.month">
                  {{ monthLabels[month.month] }}
                </th>
                <th>入力補助</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, rowIndex) in rows" :key="row.employeeId">
                <td class="sticky employee-code">{{ row.employeeCode }}</td>
                <td class="sticky employee-name">{{ row.employeeName }}</td>
                <td v-for="(month, monthIndex) in row.months" :key="month.month">
                  <v-text-field
                    v-model.number="month.draftTaxAmount"
                    type="number"
                    min="0"
                    max="10000000"
                    density="compact"
                    hide-details
                    :class="{ changed: month.currentTaxAmount !== month.draftTaxAmount, closed: month.closed }"
                    @paste="handlePaste($event, rowIndex, monthIndex)"
                  />
                </td>
                <td>
                  <v-btn size="small" variant="text" @click="copyJulyForward(row)">
                    7月以降コピー
                  </v-btn>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="text-caption mt-2">
          黄色は現在の確定値との差分、赤枠は締め済み月です。複数セルはExcelから貼り付けできます。
        </div>
      </v-card-text>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.resident-tax-grid { overflow: auto; max-height: calc(100vh - 260px); }
table { border-collapse: separate; border-spacing: 0; min-width: 1500px; width: 100%; }
th, td { border-right: 1px solid rgb(var(--v-theme-outline-variant)); border-bottom: 1px solid rgb(var(--v-theme-outline-variant)); padding: 6px; min-width: 105px; background: rgb(var(--v-theme-surface)); }
th { position: sticky; top: 0; z-index: 3; background: rgb(var(--v-theme-surface-variant)); }
.sticky { position: sticky; z-index: 2; }
.employee-code { left: 0; min-width: 130px; }
.employee-name { left: 130px; min-width: 180px; }
th.sticky { z-index: 4; }
:deep(.changed .v-field) { background: rgb(var(--v-theme-warning), .18); }
:deep(.closed .v-field) { outline: 1px solid rgb(var(--v-theme-error)); }
</style>
