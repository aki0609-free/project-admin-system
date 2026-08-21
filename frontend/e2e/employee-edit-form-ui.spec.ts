import { expect, test } from '@playwright/test'
import { E2E_EMPLOYEE_NAME } from './support/business-fixture'

test('employee basic, payroll and contract tabs use the shared form layout', async ({ page }) => {
  await page.goto('/employee/information')
  await page.getByText(E2E_EMPLOYEE_NAME, { exact: true }).click()

  const dialog = page.getByRole('dialog').filter({
    has: page.getByRole('heading', { name: '従業員情報編集', exact: true }),
  })
  await expect(dialog.getByLabel('社員コード', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('氏名', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('メール', { exact: true })).toBeVisible()

  await dialog.getByRole('button', { name: '給与・税金', exact: true }).click()
  await expect(dialog.getByLabel('税区分', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('住民税月額', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('所得税計算', { exact: true })).toBeVisible()

  await dialog.getByRole('button', { name: '契約情報', exact: true }).click()
  await expect(dialog.getByLabel('契約開始日', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('給与形態', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('契約メモ', { exact: true })).toBeVisible()

  await dialog.getByRole('button', { name: '閉じる', exact: true }).click()
  await expect(dialog).not.toBeVisible()
})

test('employee batch toolbar opens the shared parameter dialog on a narrow viewport', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/employee/information')

  await page.getByRole('button', { name: '個別日別給与明細', exact: true }).click()

  const dialog = page.getByRole('dialog').filter({
    has: page.getByRole('heading', { name: '個別日別給与明細', exact: true }),
  })
  await expect(dialog).toBeVisible()
  await expect(dialog.getByText('PRINT_DAILY_PAY_SLIP', { exact: false })).toBeVisible()
  await expect(dialog.getByLabel('支払日', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('従業員', { exact: true })).toBeVisible()
  await expect(dialog.getByRole('button', { name: '閉じる', exact: true })).toBeVisible()

  const bounds = await dialog.boundingBox()
  expect(bounds).not.toBeNull()
  if (!bounds) throw new Error('Batch dialog bounds could not be measured')
  expect(bounds.x).toBeGreaterThanOrEqual(0)
  expect(bounds.x + bounds.width).toBeLessThanOrEqual(390)
  expect(bounds.y + bounds.height).toBeLessThanOrEqual(844)

  await dialog.getByRole('button', { name: '閉じる', exact: true }).click()
  await expect(dialog).not.toBeVisible()
})
