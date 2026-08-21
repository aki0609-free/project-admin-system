import { expect, test } from '@playwright/test'

test('company profile uses the shared dialog and form layout', async ({ page }) => {
  const username = process.env.E2E_USERNAME ?? 'playwright_local'

  await page.goto('/')
  await page.getByRole('button', { name: username, exact: true }).click()
  await page.getByRole('listitem').filter({ hasText: '会社情報' }).click()

  const dialog = page.getByRole('dialog')
  await expect(dialog.getByRole('heading', { name: '会社情報', exact: true })).toBeVisible()
  await expect(dialog.getByText('会社概要', { exact: true })).toBeVisible()

  await dialog.getByRole('button', { name: '編集', exact: true }).click()
  await expect(dialog.getByLabel('会社コード', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('会社名', { exact: true })).toBeVisible()
  await expect(dialog.getByRole('button', { name: '保存', exact: true })).toBeVisible()

  await dialog.getByRole('button', { name: '請求書設定', exact: true }).click()
  await expect(dialog.getByLabel('請求書備考', { exact: true })).toBeVisible()

  await dialog.getByRole('button', { name: '許認可・事業内容', exact: true }).click()
  await expect(dialog.getByLabel('事業内容（1行につき1項目）', { exact: true })).toBeVisible()
  await expect(
    dialog.getByLabel('許認可・資格情報（1行につき1項目）', { exact: true }),
  ).toBeVisible()

  await dialog.getByRole('button', { name: 'キャンセル', exact: true }).click()
  await expect(dialog.getByRole('button', { name: '編集', exact: true })).toBeVisible()
  await dialog.getByRole('button', { name: '閉じる', exact: true }).click()
  await expect(dialog).not.toBeVisible()
})
