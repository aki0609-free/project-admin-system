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
  await expect(dialog.getByLabel('有効', { exact: true })).toHaveCount(0)
  await expect(dialog.getByRole('button', { name: '保存', exact: true })).toBeVisible()

  await dialog.getByRole('button', { name: '請求書設定', exact: true }).click()
  await expect(dialog.getByLabel('請求書備考', { exact: true })).toBeVisible()
  await dialog.getByLabel('適格請求書発行事業者登録番号', { exact: true }).fill('12345')
  await dialog.getByRole('button', { name: '保存', exact: true }).click()
  await expect(dialog.getByText('登録番号はTと13桁の数字で入力してください')).toBeVisible()

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

test('company profile accepts a comma-separated capital amount and dismisses success notice', async ({
  page,
}) => {
  const username = process.env.E2E_USERNAME ?? 'playwright_local'

  await page.goto('/')
  await page.getByRole('button', { name: username, exact: true }).click()
  await page.getByRole('listitem').filter({ hasText: '会社情報' }).click()

  const dialog = page.getByRole('dialog')
  await dialog.getByRole('button', { name: '編集', exact: true }).click()
  await dialog.getByLabel('資本金（円）', { exact: true }).fill('10,000,000')
  await dialog.getByRole('button', { name: '保存', exact: true }).click()

  const successNotice = dialog.getByText('会社情報を保存しました。', { exact: true })
  await expect(successNotice).toBeVisible()
  await expect(dialog.getByText('10,000,000円', { exact: true })).toBeVisible()
  await expect(successNotice).not.toBeVisible({ timeout: 6000 })
})
