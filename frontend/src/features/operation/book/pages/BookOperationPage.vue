<script setup lang="ts">
import { computed, defineAsyncComponent, ref, watch } from 'vue'
import ListDetailPageLayout from '@/toolbox/pages/ListDetailPageLayout.vue'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import { useOperationExcelBooksQuery } from '../api/useOperationExcelBooksQuery'
import { useGenerateSpreadsheetLedgerMutation } from '../api/useGenerateSpreadsheetLedgerMutation'
import { useGenerateSelectedSpreadsheetLedgersMutation } from '../api/useGenerateSelectedSpreadsheetLedgersMutation'
import { useSpreadsheetLedgerSelectionQuery } from '../api/useSpreadsheetLedgerSelectionQuery'
import type {
  OperationExcelBook,
  SpreadsheetLedgerGenerateResponse,
} from '../types/operationBookTypes'

const GeneratedSpreadsheetLedgerDialog = defineAsyncComponent(
  () => import('../components/GeneratedSpreadsheetLedgerDialog.vue'),
)

const FISCAL_YEAR_START_MONTH = 8
const currentMonth = currentBusinessMonth()
const currentYearMonth = parseYearMonth(currentMonth)
const selectedFiscalYear = ref<number>(
  currentYearMonth.month >= FISCAL_YEAR_START_MONTH
    ? currentYearMonth.year
    : currentYearMonth.year - 1,
)
const selectedMonth = ref(currentMonth)
const targetMonth = computed(() => selectedMonth.value)
const selectedBookCode = ref<string | null>(null)
const generatedResult =
  ref<SpreadsheetLedgerGenerateResponse | null>(null)
const previewDialog = ref(false)
const selectionDialog = ref(false)
const selectionBookCode = ref<string | null>(null)
const selectedSelectionValues = ref<string[]>([])
const operationMessage = ref('')
const operationError = ref(false)

const booksQuery = useOperationExcelBooksQuery()
const generateMutation = useGenerateSpreadsheetLedgerMutation()
const generateSelectedMutation =
  useGenerateSelectedSpreadsheetLedgersMutation()
const selectionQuery = useSpreadsheetLedgerSelectionQuery(
  selectionBookCode,
  targetMonth,
)

const leftToolbarItems = computed<ToolbarItem[]>(() => [])
const rightToolbarItems = computed<ToolbarItem[]>(() => [
  {
    type: 'button',
    label: '再読込',
    color: 'secondary',
    disabled: booksQuery.isLoading.value,
    onClick: () => booksQuery.refetch(),
  },
])

const generating = computed(() =>
  generateMutation.isPending.value
  || generateSelectedMutation.isPending.value,
)
const selectionBook = computed(() =>
  booksQuery.books.value.find(
    book => book.bookCode === selectionBookCode.value,
  ) ?? null,
)
const allSelectionValues = computed(() =>
  selectionQuery.selection.value?.options.map(option => option.value)
  ?? [],
)
const allSelected = computed({
  get: () =>
    allSelectionValues.value.length > 0
    && selectedSelectionValues.value.length
      === allSelectionValues.value.length,
  set: (value: boolean) => {
    selectedSelectionValues.value = value
      ? [...allSelectionValues.value]
      : []
  },
})
const fiscalYearItems = computed(() => {
  const currentFiscalYear =
    currentYearMonth.month >= FISCAL_YEAR_START_MONTH
      ? currentYearMonth.year
      : currentYearMonth.year - 1
  return Array.from({ length: 9 }, (_, index) => {
    const value = currentFiscalYear + 2 - index
    return { title: `${value}年度`, value }
  })
})
const fiscalMonthItems = computed(() =>
  Array.from({ length: 12 }, (_, index) => {
    const monthOffset = FISCAL_YEAR_START_MONTH - 1 + index
    const year = selectedFiscalYear.value + Math.floor(monthOffset / 12)
    const month = monthOffset % 12 + 1
    const value = `${year}-${String(month).padStart(2, '0')}`
    return {
      title: `${year}年${month}月`,
      value,
    }
  }),
)
async function generate(book: OperationExcelBook) {
  if (!targetMonth.value || !book.generationReady) return

  if (book.selection.mode !== 'NONE') {
    selectionBookCode.value = book.bookCode
    selectedSelectionValues.value = []
    selectionDialog.value = true
    return
  }

  selectedBookCode.value = book.bookCode
  operationMessage.value = ''
  operationError.value = false

  try {
    generatedResult.value = await generateMutation.mutateAsync({
      bookCode: book.bookCode,
      request: {
        targetMonth: targetMonth.value,
      },
    }) as SpreadsheetLedgerGenerateResponse
    operationMessage.value =
      `${book.bookName}を生成し、生成帳票へ保存しました。`
    previewDialog.value = true
  } catch {
    operationError.value = true
    operationMessage.value =
      '台帳の生成に失敗しました。テンプレート変数と対象データを確認してください。'
  } finally {
    selectedBookCode.value = null
  }
}

async function generateSelected() {
  const book = selectionBook.value
  if (!book || selectedSelectionValues.value.length === 0) return

  selectedBookCode.value = book.bookCode
  operationMessage.value = ''
  operationError.value = false
  try {
    const results = await generateSelectedMutation.mutateAsync({
      bookCode: book.bookCode,
      request: {
        targetMonth: targetMonth.value,
        selectionValues: selectedSelectionValues.value,
      },
    }) as SpreadsheetLedgerGenerateResponse[]
    generatedResult.value = combineGeneratedResults(results)
    operationMessage.value =
      `${book.bookName}を${results.length}件生成し、従業員別に保存しました。`
    selectionDialog.value = false
    previewDialog.value = true
  } catch {
    operationError.value = true
    operationMessage.value =
      '台帳の生成に失敗しました。対象者と対象月のデータを確認してください。'
  } finally {
    selectedBookCode.value = null
  }
}

function combineGeneratedResults(
  results: SpreadsheetLedgerGenerateResponse[],
): SpreadsheetLedgerGenerateResponse | null {
  if (results.length === 0) return null
  if (results.length === 1) return results[0] ?? null

  const first = results[0]
  if (!first) return null
  const combined = structuredClone(first)
  const targetWorkbook = workbookNode(combined.workbook)
  targetWorkbook.sheets = results.flatMap(result => {
    const source = workbookNode(result.workbook)
    return Array.isArray(source.sheets)
      ? structuredClone(source.sheets)
      : []
  })
  combined.rowCount = results.reduce(
    (total, result) => total + result.rowCount,
    0,
  )
  combined.workbookBytes = results.reduce(
    (total, result) => total + result.workbookBytes,
    0,
  )
  combined.generationDurationMs = results.reduce(
    (total, result) => total + result.generationDurationMs,
    0,
  )
  combined.storagePath = `${results.length}件を従業員別に保存済み`
  combined.selectionValue = null
  combined.editable = false
  return combined
}

function workbookNode(workbook: Record<string, unknown>) {
  const wrapped = workbook.Workbook
  if (wrapped && typeof wrapped === 'object') {
    return wrapped as Record<string, unknown> & { sheets?: unknown[] }
  }
  return workbook as Record<string, unknown> & { sheets?: unknown[] }
}

function displayValue(
  option: { displayValues: Record<string, unknown> },
  columnName: string,
) {
  return String(option.displayValues[columnName] ?? '')
}

function currentBusinessMonth(): string {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: 'Asia/Tokyo',
    year: 'numeric',
    month: '2-digit',
  }).formatToParts(new Date())
  const year = parts.find(part => part.type === 'year')?.value
  const month = parts.find(part => part.type === 'month')?.value
  return `${year}-${month}`
}

function parseYearMonth(value: string) {
  const [yearText = '1970', monthText = '1'] = value.split('-')
  return {
    year: Number(yearText),
    month: Number(monthText),
  }
}

watch(selectedFiscalYear, () => {
  if (
    !fiscalMonthItems.value.some(
      item => item.value === selectedMonth.value,
    )
  ) {
    selectedMonth.value = fiscalMonthItems.value[0]?.value ?? currentMonth
  }
})
</script>

<template>
  <ListDetailPageLayout
    title="台帳管理"
    description="対象月のデータをSpreadsheetテンプレートへ展開し、生成台帳を確認します。"
    :left-toolbar-items="leftToolbarItems"
    :right-toolbar-items="rightToolbarItems"
  >
    <template #search>
      <div class="book-search">
        <v-select
          v-model="selectedFiscalYear"
          :items="fiscalYearItems"
          label="年度"
          variant="outlined"
          density="compact"
          hide-details
          prepend-inner-icon="mdi-calendar-range"
        />
        <v-select
          v-model="selectedMonth"
          :items="fiscalMonthItems"
          label="対象月"
          variant="outlined"
          density="compact"
          hide-details
          prepend-inner-icon="mdi-calendar-month"
        />
      </div>
    </template>

    <v-alert
      v-if="operationMessage"
      :type="operationError ? 'error' : 'success'"
      variant="tonal"
      density="compact"
      closable
      class="mb-3"
      @click:close="operationMessage = ''"
    >
      {{ operationMessage }}
    </v-alert>

    <v-alert
      v-if="booksQuery.isError.value"
      type="error"
      variant="tonal"
      density="compact"
      class="mb-3"
    >
      台帳マスタの取得に失敗しました。
    </v-alert>

    <v-progress-linear
      v-if="booksQuery.isLoading.value"
      indeterminate
      color="primary"
      class="mb-3"
    />

    <v-table class="book-table" density="comfortable">
      <thead>
        <tr>
          <th>台帳名</th>
          <th>Book Code</th>
          <th>データソース</th>
          <th>生成方式</th>
          <th class="book-table__action">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="book in booksQuery.books.value" :key="book.id">
          <td class="font-weight-bold">{{ book.bookName }}</td>
          <td>
            <v-chip size="small" variant="tonal">
              {{ book.bookCode }}
            </v-chip>
          </td>
          <td>{{ book.dataSourceCode }}</td>
          <td>
            <v-chip
              size="small"
              variant="tonal"
              :color="
                !book.generationReady
                  ? 'error'
                  : book.generationMode === 'CODE'
                    ? 'info'
                    : 'success'
              "
            >
              {{
                !book.generationReady
                  ? '未設定'
                  : book.generationMode === 'CODE'
                    ? 'コード生成'
                    : 'テンプレート'
              }}
            </v-chip>
          </td>
          <td class="book-table__action">
            <v-btn
              color="primary"
              size="small"
              prepend-icon="mdi-table-large"
              :loading="
                generating && selectedBookCode === book.bookCode
              "
              :disabled="
                generating
                  || !targetMonth
                  || !book.generationReady
              "
              @click="generate(book)"
            >
              生成・確認
            </v-btn>
          </td>
        </tr>
        <tr v-if="!booksQuery.isLoading.value && booksQuery.books.value.length === 0">
          <td colspan="5" class="text-center text-medium-emphasis py-8">
            有効な台帳マスタがありません。
          </td>
        </tr>
      </tbody>
    </v-table>

    <v-alert
      type="info"
      variant="tonal"
      density="compact"
      class="mt-3"
    >
      生成した台帳は書類管理の「生成帳票」にJSON形式で保存されます。
    </v-alert>

    <GeneratedSpreadsheetLedgerDialog
      v-model="previewDialog"
      :result="generatedResult"
    />

    <v-dialog v-model="selectionDialog" max-width="880">
      <v-card>
        <v-card-title>
          {{ selectionBook?.bookName }}：生成対象を選択
        </v-card-title>
        <v-card-subtitle>
          {{ targetMonth }}・選択した対象ごとに台帳ファイルを生成します。
        </v-card-subtitle>

        <v-card-text>
          <v-progress-linear
            v-if="selectionQuery.isLoading.value"
            indeterminate
            color="primary"
            class="mb-3"
          />
          <v-alert
            v-if="selectionQuery.isError.value"
            type="error"
            variant="tonal"
            density="compact"
            class="mb-3"
          >
            対象一覧を取得できませんでした。
          </v-alert>

          <v-checkbox
            v-if="selectionQuery.selection.value?.allowSelectAll"
            v-model="allSelected"
            label="全件選択（全員分を生成・印刷）"
            hide-details
            density="compact"
          />

          <v-table density="compact" class="selection-table">
            <thead>
              <tr>
                <th class="selection-table__check">選択</th>
                <th
                  v-for="column in selectionQuery.selection.value?.columns"
                  :key="column.columnName"
                >
                  {{ column.displayName }}
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="option in selectionQuery.selection.value?.options"
                :key="option.value"
              >
                <td>
                  <v-checkbox
                    v-model="selectedSelectionValues"
                    :value="option.value"
                    hide-details
                    density="compact"
                  />
                </td>
                <td
                  v-for="column in selectionQuery.selection.value?.columns"
                  :key="column.columnName"
                >
                  {{ displayValue(option, column.columnName) }}
                </td>
              </tr>
              <tr
                v-if="
                  !selectionQuery.isLoading.value
                    && !selectionQuery.selection.value?.options.length
                "
              >
                <td
                  :colspan="
                    (selectionQuery.selection.value?.columns.length ?? 0) + 1
                  "
                  class="text-center text-medium-emphasis py-6"
                >
                  対象月の生成対象がありません。
                </td>
              </tr>
            </tbody>
          </v-table>
        </v-card-text>

        <v-card-actions>
          <span class="text-caption text-medium-emphasis ml-2">
            {{ selectedSelectionValues.length }}件選択中
          </span>
          <v-spacer />
          <v-btn @click="selectionDialog = false">キャンセル</v-btn>
          <v-btn
            color="primary"
            :loading="generateSelectedMutation.isPending.value"
            :disabled="selectedSelectionValues.length === 0"
            @click="generateSelected"
          >
            生成・確認
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </ListDetailPageLayout>
</template>

<style scoped>
.book-search {
  display: grid;
  grid-template-columns: minmax(150px, 180px) minmax(180px, 220px);
  gap: 12px;
  max-width: 420px;
}

.selection-table {
  max-height: 55vh;
  overflow: auto;
}

.selection-table__check {
  width: 72px;
}

.book-table {
  border: 1px solid rgb(var(--v-theme-outline-variant));
  border-radius: 8px;
}

.book-table__action {
  width: 180px;
  text-align: right;
}
</style>
