import { expect, test } from '@playwright/test'

test('year-end adjustment is managed from the closing operation menu', async ({ page }) => {
  await page.goto('/operation/year-end-adjustments')

  await expect(page).toHaveURL(/\/operation\/year-end-adjustments$/)
  await expect(page.getByRole('heading', { name: '年末調整', exact: true })).toBeVisible()
  await expect(page.getByRole('heading', { name: '年末調整一覧', exact: true })).toBeVisible()
  await expect(page.getByLabel('対象年度', { exact: true })).toBeVisible()
  await expect(page.getByLabel('従業員', { exact: true })).toBeVisible()
  await expect(page.getByLabel('反映対象月', { exact: true })).toBeVisible()
  await expect(page.getByLabel('年末調整額', { exact: true })).toBeVisible()
})
