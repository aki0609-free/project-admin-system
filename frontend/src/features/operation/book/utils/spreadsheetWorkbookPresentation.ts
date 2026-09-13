type SpreadsheetStyle = Record<string, unknown> & {
  border?: unknown
  borderTop?: unknown
  borderRight?: unknown
  borderBottom?: unknown
  borderLeft?: unknown
}

type SpreadsheetCell = {
  style?: SpreadsheetStyle
}

type SpreadsheetRow = {
  cells?: SpreadsheetCell[]
}

type SpreadsheetSheet = {
  rows?: SpreadsheetRow[]
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
    for (const row of sheet.rows ?? []) {
      for (const cell of row.cells ?? []) {
        normalizeCellBorder(cell)
      }
    }
  }

  return prepared as T
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
