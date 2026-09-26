type SpreadsheetCell = {
  index?: number
  value?: unknown
  formattedText?: string
  formula?: string
  rowSpan?: number
  colSpan?: number
  style?: Record<string, unknown>
  format?: string
}

type SpreadsheetRow = {
  index?: number
  height?: number
  cells?: Array<SpreadsheetCell | null>
}

type SpreadsheetSheet = {
  name?: string
  selectedRange?: string
  columns?: Array<{ width?: number } | null>
  rows?: Array<SpreadsheetRow | null>
}

type SpreadsheetWorkbook = {
  Workbook?: { sheets?: SpreadsheetSheet[] }
  sheets?: SpreadsheetSheet[]
  projectAdminMetadata?: {
    paperSize?: string
    orientation?: string
  }
}

export function printSpreadsheetRange(
  source: Record<string, unknown>,
  activeSheetIndex: number,
  selectedRange: string,
) {
  const workbook = source as SpreadsheetWorkbook
  const sheets = workbook.Workbook?.sheets ?? workbook.sheets ?? []
  const sheet = sheets[activeSheetIndex]
  if (!sheet) {
    throw new Error('印刷対象のシートが見つかりません。')
  }

  const [startRow, startColumn, endRow, endColumn] = rangeIndexes(
    selectedRange || sheet.selectedRange || 'A1:A1',
  )
  const html = buildPrintHtml(
    sheet,
    startRow,
    startColumn,
    endRow,
    endColumn,
    workbook.projectAdminMetadata,
  )
  printHtml(html)
}

function buildPrintHtml(
  sheet: SpreadsheetSheet,
  startRow: number,
  startColumn: number,
  endRow: number,
  endColumn: number,
  print: SpreadsheetWorkbook['projectAdminMetadata'],
) {
  const columns = Array.from(
    { length: endColumn - startColumn + 1 },
    (_, offset) => {
      const width = sheet.columns?.[startColumn + offset]?.width ?? 64
      return `<col style="width:${Math.max(24, width)}px">`
    },
  ).join('')
  const rowMap = indexedRows(sheet.rows ?? [])
  const rows: string[] = []

  for (let rowIndex = startRow; rowIndex <= endRow; rowIndex++) {
    const sourceRow = rowMap.get(rowIndex)
    const cellMap = indexedCells(sourceRow?.cells ?? [])
    const cells: string[] = []
    for (
      let columnIndex = startColumn;
      columnIndex <= endColumn;
      columnIndex++
    ) {
      const cell = cellMap.get(columnIndex)
      if ((cell?.rowSpan ?? 0) < 0 || (cell?.colSpan ?? 0) < 0) continue

      const rowSpan = Math.min(
        Math.max(1, cell?.rowSpan ?? 1),
        endRow - rowIndex + 1,
      )
      const overflowAcrossSelection = columnIndex === startColumn
        && cell?.style?.overflow === 'visible'
      const colSpan = overflowAcrossSelection
        ? endColumn - columnIndex + 1
        : Math.min(
            Math.max(1, cell?.colSpan ?? 1),
            endColumn - columnIndex + 1,
          )
      cells.push(
        `<td${rowSpan > 1 ? ` rowspan="${rowSpan}"` : ''}`
        + `${colSpan > 1 ? ` colspan="${colSpan}"` : ''}`
        + ` style="${cellStyle(cell?.style)}">`
        + `${escapeHtml(displayValue(cell))}</td>`,
      )
      columnIndex += colSpan - 1
    }
    const height = sourceRow?.height
      ? ` style="height:${sourceRow.height}px"`
      : ''
    rows.push(`<tr${height}>${cells.join('')}</tr>`)
  }

  const paperSize = print?.paperSize?.toUpperCase() ?? 'A4'
  const orientation = print?.orientation?.toUpperCase() === 'LANDSCAPE'
    ? 'landscape'
    : 'portrait'
  return `<!doctype html>
<html lang="ja"><head><meta charset="utf-8"><title>${escapeHtml(
    sheet.name ?? '台帳',
  )}</title><style>
@page { size: ${paperSize} ${orientation}; margin: 8mm; }
html, body { margin: 0; padding: 0; }
table { border-collapse: collapse; table-layout: fixed; width: max-content; }
td { box-sizing: border-box; overflow: hidden; padding: 2px 4px; }
</style></head><body><table><colgroup>${columns}</colgroup><tbody>${
    rows.join('')
  }</tbody></table></body></html>`
}

function indexedRows(rows: Array<SpreadsheetRow | null>) {
  const result = new Map<number, SpreadsheetRow>()
  rows.forEach((row, offset) => {
    if (row) result.set(row.index ?? offset, row)
  })
  return result
}

function indexedCells(cells: Array<SpreadsheetCell | null>) {
  const result = new Map<number, SpreadsheetCell>()
  cells.forEach((cell, offset) => {
    if (cell) result.set(cell.index ?? offset, cell)
  })
  return result
}

function displayValue(cell?: SpreadsheetCell) {
  if (!cell) return ''
  if (cell.formattedText != null) return cell.formattedText
  if (cell.value == null) return ''
  if (typeof cell.value !== 'number') return String(cell.value)
  if (cell.format?.includes('%')) {
    return `${new Intl.NumberFormat('ja-JP', {
      maximumFractionDigits: decimalPlaces(cell.format),
    }).format(cell.value * 100)}%`
  }
  if (cell.format?.includes(',')) {
    return new Intl.NumberFormat('ja-JP', {
      minimumFractionDigits: decimalPlaces(cell.format),
      maximumFractionDigits: decimalPlaces(cell.format),
    }).format(cell.value)
  }
  return String(cell.value)
}

function decimalPlaces(format: string) {
  const decimal = format.split('.')[1]?.replace(/[^0#].*$/, '') ?? ''
  return decimal.length
}

function cellStyle(style?: Record<string, unknown>) {
  if (!style) return ''
  const allowed = [
    'fontFamily',
    'fontSize',
    'fontWeight',
    'fontStyle',
    'color',
    'backgroundColor',
    'textAlign',
    'verticalAlign',
    'whiteSpace',
    'overflow',
    'border',
    'borderTop',
    'borderRight',
    'borderBottom',
    'borderLeft',
  ] as const
  return allowed
    .flatMap(key => {
      const value = style[key]
      return typeof value === 'string'
        ? [`${toKebabCase(key)}:${escapeCss(value)}`]
        : []
    })
    .join(';')
}

function rangeIndexes(address: string): [number, number, number, number] {
  const range = address.replace(/^.*!/, '').replaceAll('$', '')
  const [start, end = start] = range.split(':')
  if (!start || !end) throw new Error(`印刷範囲が不正です: ${address}`)
  const startIndex = cellIndexes(start)
  const endIndex = cellIndexes(end)
  return [
    Math.min(startIndex[0], endIndex[0]),
    Math.min(startIndex[1], endIndex[1]),
    Math.max(startIndex[0], endIndex[0]),
    Math.max(startIndex[1], endIndex[1]),
  ]
}

function cellIndexes(address: string): [number, number] {
  const match = /^([A-Za-z]+)(\d+)$/.exec(address.trim())
  if (!match) throw new Error(`印刷範囲が不正です: ${address}`)
  const columnAddress = match[1]
  const rowAddress = match[2]
  if (!columnAddress || !rowAddress) {
    throw new Error(`印刷範囲が不正です: ${address}`)
  }
  let column = 0
  for (const character of columnAddress.toUpperCase()) {
    column = column * 26 + character.charCodeAt(0) - 64
  }
  return [Number(rowAddress) - 1, column - 1]
}

function printHtml(html: string) {
  // クリック直後に印刷用ウィンドウを確保する。非表示iframeを遅延印刷
  // するとブラウザに抑止され、何も起きないように見える場合がある。
  const printWindow = window.open('', '_blank')
  if (!printWindow) {
    throw new Error(
      '印刷画面を開けませんでした。ポップアップを許可してください。',
    )
  }
  printWindow.document.open()
  printWindow.document.write(html)
  printWindow.document.close()
  window.setTimeout(() => {
    printWindow.focus()
    printWindow.print()
  }, 250)
}

function toKebabCase(value: string) {
  return value.replace(/[A-Z]/g, match => `-${match.toLowerCase()}`)
}

function escapeHtml(value: unknown) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function escapeCss(value: string) {
  return value.replace(/[;{}<>]/g, '')
}
