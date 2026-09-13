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
})
