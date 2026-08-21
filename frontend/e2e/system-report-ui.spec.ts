import { expect, test } from '@playwright/test'

const requireBox = <T>(value: T | null, label: string): T => {
  if (value == null) {
    throw new Error(`${label}の表示位置を取得できませんでした。`)
  }
  return value
}

test('report management uses shared page, dialogs, forms, and toolbars', async ({ page }) => {
  await page.goto('/system/report')

  await expect(page.getByRole('heading', { name: '帳票管理' })).toBeVisible()
  await expect(page.getByText('PDF・CSV・EXCELを生成する帳票定義、署名、出力履歴を管理します。')).toBeVisible()

  const createReportButton = page.getByRole('button', { name: '新規作成', exact: true })
  await expect(createReportButton).toBeVisible()
  await createReportButton.click()

  let dialog = page.getByRole('dialog')
  await expect(dialog.getByRole('heading', { name: '帳票定義新規作成' })).toBeVisible()
  await expect(dialog.getByLabel('帳票コード', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('帳票名', { exact: true })).toBeVisible()

  let closeButton = dialog.getByRole('button', { name: '閉じる', exact: true })
  let saveButton = dialog.getByRole('button', { name: '保存', exact: true })
  let closeBox = requireBox(await closeButton.boundingBox(), '帳票定義の閉じるボタン')
  let saveBox = requireBox(await saveButton.boundingBox(), '帳票定義の保存ボタン')
  expect(closeBox.x).toBeLessThan(saveBox.x)

  await closeButton.click()
  await expect(dialog).toBeHidden()

  await page.getByRole('button', { name: 'Signature', exact: true }).click()
  await page.getByRole('button', { name: '新規作成', exact: true }).click()

  dialog = page.getByRole('dialog')
  await expect(dialog.getByRole('heading', { name: 'Signature新規作成' })).toBeVisible()
  await expect(dialog.getByLabel('署名名', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('種別', { exact: true })).toBeVisible()

  closeButton = dialog.getByRole('button', { name: '閉じる', exact: true })
  saveButton = dialog.getByRole('button', { name: '保存', exact: true })
  closeBox = requireBox(await closeButton.boundingBox(), '署名の閉じるボタン')
  saveBox = requireBox(await saveButton.boundingBox(), '署名の保存ボタン')
  expect(closeBox.x).toBeLessThan(saveBox.x)

  await closeButton.click()
  await expect(dialog).toBeHidden()

  await page.getByRole('button', { name: '手動印刷', exact: true }).click()
  await expect(page.getByLabel('帳票', { exact: true })).toBeVisible()
  await expect(page.getByLabel('保存名', { exact: true })).toBeVisible()
})
