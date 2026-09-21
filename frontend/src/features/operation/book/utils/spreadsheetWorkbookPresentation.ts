type SpreadsheetStyle = Record<string, unknown> & {
  border?: unknown
  borderTop?: unknown
  borderRight?: unknown
  borderBottom?: unknown
  borderLeft?: unknown
}

type SpreadsheetCell = {
  index?: number
  style?: SpreadsheetStyle
}

type SpreadsheetRow = {
  index?: number
  cells?: SpreadsheetCell[]
}

type SpreadsheetSheet = {
  colCount?: number
  rowCount?: number
  columns?: Array<{ index?: number }>
  rows?: SpreadsheetRow[]
  usedRange?: {
    colIndex?: number
    rowIndex?: number
  }
}

type SpreadsheetWorkbook = Record<string, unknown> & {
  Workbook?: SpreadsheetWorkbook
  sheets?: SpreadsheetSheet[]
}

const BORDER_EDGES = [
  'borderTop',
  'borderRight',
  'borderBottom',
  'borderLeft',
] as const

/**
 * Syncfusionの画面再描画と印刷で同じ罫線になるよう、border一括指定を
 * 辺ごとの指定へ展開した表示用コピーを作る。
 */
export function prepareSpreadsheetWorkbook<T extends Record<string, unknown>>(
  source: T,
): T {
  const prepared = structuredClone(source) as T & SpreadsheetWorkbook
  const workbook = prepared.Workbook ?? prepared

  for (const sheet of workbook.sheets ?? []) {
    ensureFiniteSheetRange(sheet)
    for (const row of sheet.rows ?? []) {
      for (const cell of row.cells ?? []) {
        normalizeCellBorder(cell)
      }
    }
  }

  return prepared as T
}

/**
 * Syncfusionの有限スクロールはcolCount／rowCount未指定時に既定範囲で
 * 打ち切られるため、テンプレートの実データ範囲から明示的に補完する。
 */
function ensureFiniteSheetRange(sheet: SpreadsheetSheet) {
  const usedColumnCount = (sheet.usedRange?.colIndex ?? -1) + 1
  const definedColumnCount = maxIndexedPosition(sheet.columns)
  const cellColumnCount = Math.max(
    0,
    ...(sheet.rows ?? []).map(row => maxIndexedPosition(row.cells)),
  )
  sheet.colCount = Math.max(
    sheet.colCount ?? 0,
    usedColumnCount,
    definedColumnCount,
    cellColumnCount,
  )

  const usedRowCount = (sheet.usedRange?.rowIndex ?? -1) + 1
  const definedRowCount = maxIndexedPosition(sheet.rows)
  sheet.rowCount = Math.max(
    sheet.rowCount ?? 0,
    usedRowCount,
    definedRowCount,
  )
}

function maxIndexedPosition(
  entries: Array<{ index?: number }> | undefined,
): number {
  if (!entries?.length) return 0

  return Math.max(
    ...entries.map((entry, position) =>
      (entry.index ?? position) + 1),
  )
}

function normalizeCellBorder(cell: SpreadsheetCell) {
  const style = cell.style
  if (!style || typeof style.border !== 'string') return

  for (const edge of BORDER_EDGES) {
    if (typeof style[edge] !== 'string') {
      style[edge] = style.border
    }
  }
  delete style.border
}
