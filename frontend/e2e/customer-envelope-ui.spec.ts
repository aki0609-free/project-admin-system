import { expect, test } from '@playwright/test'

test('customer envelope print dialog uses the shared form layout', async ({ page }) => {
  await page.goto('/customer/information')

  await expect(page.getByRole('heading', { name: '顧客管理', exact: true })).toBeVisible()
  await page.getByRole('button', { name: '封筒宛名印刷', exact: true }).click()

  const dialog = page.getByRole('dialog')
  await expect(dialog.getByText('封筒宛名印刷', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('印刷する企業', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('封筒タイプ', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('封筒スタンプ', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('敬称', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('フォント', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('文字サイズ', { exact: true })).toBeVisible()
  await expect(dialog.getByText('印刷イメージ', { exact: true })).toBeVisible()
  await expect(dialog.getByRole('button', { name: '宛名印刷', exact: true })).toBeDisabled()

  await dialog.getByRole('button', { name: 'キャンセル', exact: true }).click()
  await expect(dialog).not.toBeVisible()
})
