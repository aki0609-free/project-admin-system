import { expect, test } from '@playwright/test'

const requireBox = <T>(value: T | null, label: string): T => {
  if (value == null) {
    throw new Error(`${label}の表示位置を取得できませんでした。`)
  }
  return value
}

test('excel book master uses shared dialog without changing spreadsheet entry point', async ({ page }) => {
  await page.goto('/system/excelbook')

  await expect(page.getByRole('heading', { name: '台帳マスタ' })).toBeVisible()

  const createButton = page.getByRole('button', { name: '新規', exact: true })
  const reloadButton = page.getByRole('button', { name: '再読込', exact: true })
  const createBox = requireBox(await createButton.boundingBox(), '新規ボタン')
  const reloadBox = requireBox(await reloadButton.boundingBox(), '再読込ボタン')
  expect(createBox.x).toBeLessThan(reloadBox.x)

  await createButton.click()

  let dialog = page.getByRole('dialog').filter({ hasText: '台帳マスタ 新規作成' })
  await expect(dialog).toBeVisible()
  await expect(dialog.getByLabel('台帳コード', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('データソース', { exact: true })).toBeVisible()
  await expect(
    dialog.getByRole('button', { name: 'Spreadsheetテンプレート編集', exact: true }),
  ).toBeDisabled()
  await dialog.getByRole('button', { name: 'キャンセル', exact: true }).click()
  await expect(dialog).toBeHidden()

  const existingRow = page
    .getByRole('row')
    .filter({ hasText: 'RECEIPT_CONFIRMATION' })
  await expect(existingRow).toHaveCount(1)
  await existingRow.click()

  dialog = page.getByRole('dialog').filter({ hasText: '台帳マスタ：' })
  await expect(dialog).toBeVisible()
  await expect(dialog.getByLabel('台帳コード', { exact: true })).toHaveValue(
    'RECEIPT_CONFIRMATION',
  )
  await expect(
    dialog.getByRole('button', { name: 'Spreadsheetテンプレート編集', exact: true }),
  ).toBeEnabled()
  await expect(dialog.getByRole('button', { name: '削除', exact: true })).toBeEnabled()

  const templateBox = requireBox(
    await dialog
      .getByRole('button', { name: 'Spreadsheetテンプレート編集', exact: true })
      .boundingBox(),
    'Spreadsheetテンプレート編集ボタン',
  )
  const saveBox = requireBox(
    await dialog.getByRole('button', { name: '保存', exact: true }).boundingBox(),
    '保存ボタン',
  )
  expect(templateBox.x).toBeLessThan(saveBox.x)

  await dialog.getByRole('button', { name: 'キャンセル', exact: true }).click()
  await expect(dialog).toBeHidden()
})

test('receipt confirmation opens its saved spreadsheet template', async ({ page }) => {
  await page.goto('/system/excelbook')

  const receiptRow = page
    .getByRole('row')
    .filter({ hasText: 'RECEIPT_CONFIRMATION' })
  await expect(receiptRow).toHaveCount(1)
  await receiptRow.click()

  const masterDialog = page
    .getByRole('dialog')
    .filter({ hasText: '台帳マスタ：入金確認表' })
  await masterDialog
    .getByRole('button', {
      name: 'Spreadsheetテンプレート編集',
      exact: true,
    })
    .click()

  const templateDialog = page
    .getByRole('dialog')
    .filter({ hasText: 'Spreadsheetテンプレート：入金確認表' })
  await expect(templateDialog).toBeVisible()
  await expect(templateDialog).toContainText(
    'Book Code: RECEIPT_CONFIRMATION',
  )
  await expect(templateDialog.getByText('テンプレート変数例')).toHaveCount(0)
  await expect(templateDialog.getByText('${name}')).toHaveCount(0)
  await expect(templateDialog.getByText('${customerName}')).toBeVisible()
  await expect(templateDialog.getByText('対象月 合計')).toBeVisible()
  await expect(templateDialog.getByText('総合計')).toBeVisible()
})
