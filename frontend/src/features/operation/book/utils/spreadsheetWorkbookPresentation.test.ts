import { describe, expect, it } from 'vitest'
import { prepareSpreadsheetWorkbook } from './spreadsheetWorkbookPresentation'

describe('prepareSpreadsheetWorkbook', () => {
  it('罫線の一括指定を四辺へ展開し、元データを変更しない', () => {
    const source = {
      Workbook: {
        sheets: [{
          rows: [{
            cells: [{
              style: {
                border: '1px solid #000000',
                borderBottom: '1px dashed #000000',
              },
            }],
          }],
        }],
      },
    }

    const result = prepareSpreadsheetWorkbook(source)
    const style = result.Workbook.sheets[0]?.rows[0]?.cells[0]?.style

    expect(style).toEqual({
      borderTop: '1px solid #000000',
      borderRight: '1px solid #000000',
      borderBottom: '1px dashed #000000',
      borderLeft: '1px solid #000000',
    })
    expect(source.Workbook.sheets[0]?.rows[0]?.cells[0]?.style)
      .toHaveProperty('border', '1px solid #000000')
  })

  it('Workbookでラップされていない形式にも対応する', () => {
    const result = prepareSpreadsheetWorkbook({
      sheets: [{
        rows: [{
          cells: [{ style: { border: '2px solid #123456' } }],
        }],
      }],
    })

    expect(result.sheets[0]?.rows[0]?.cells[0]?.style).toMatchObject({
      borderTop: '2px solid #123456',
      borderRight: '2px solid #123456',
      borderBottom: '2px solid #123456',
      borderLeft: '2px solid #123456',
    })
  })

  it('有限スクロール用の行数・列数を実データ範囲から補完する', () => {
    const result = prepareSpreadsheetWorkbook({
      Workbook: {
        sheets: [{
          columns: [{ index: 0 }, { index: 141 }],
          rows: [{ index: 0 }, {
            index: 91,
            cells: [{ index: 141 }],
          }],
          usedRange: {
            rowIndex: 91,
            colIndex: 141,
          },
        }],
      },
    })

    const sheet = result.Workbook.sheets[0] as Record<string, unknown>
    expect(sheet.colCount).toBe(142)
    expect(sheet.rowCount).toBe(92)
  })
})
