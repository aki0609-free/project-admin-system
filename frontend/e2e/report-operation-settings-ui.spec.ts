import { expect, test } from '@playwright/test'

test('closing and preview report settings are consolidated into report management', async ({ page }) => {
  await page.goto('/system/report')

  await expect(page.getByRole('heading', { name: '帳票管理', exact: true })).toBeVisible()
  await page.getByRole('button', { name: '運用設定', exact: true }).click()

  await expect(page.getByRole('button', { name: '締め帳票', exact: true })).toBeVisible()
  await expect(page.getByRole('heading', { name: '月次締め帳票', exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: '締め帳票設定を保存', exact: true })).toBeVisible()

  await page.getByRole('button', { name: 'プレビュー帳票', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'プレビュー帳票', exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: 'プレビュー帳票を追加', exact: true })).toBeVisible()
})
