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
  await expect(dialog.getByText('印刷イメージ', { exact: true })).toBeVisible()
  await expect(dialog.getByRole('button', { name: '宛名印刷', exact: true })).toBeDisabled()

  await dialog.getByRole('button', { name: 'キャンセル', exact: true }).click()
  await expect(dialog).not.toBeVisible()
})

test('customer envelope print generates a PDF preview', async ({ page }) => {
  await page.goto('/customer/information')
  await page.getByRole('button', { name: '封筒宛名印刷', exact: true }).click()

  const dialog = page.getByRole('dialog')
  await dialog.locator('.v-select').first().click()
  await page.getByRole('option').first().click()

  const executeResponse = page.waitForResponse(response =>
    response.url().includes('/api/system/batch/execute/PRINT_ENVELOPE_NAGA3'),
  )
  await dialog.getByRole('button', { name: '宛名印刷', exact: true }).click()
  expect((await executeResponse).status()).toBe(200)

  await expect(page.getByRole('button', { name: '印刷', exact: true })).toBeVisible({
    timeout: 30_000,
  })
  await expect(page.locator('iframe.pdf-frame')).toHaveAttribute('src', /^blob:/)
})

test('customer envelope print generates a Kaku2 PDF preview', async ({ page }) => {
  await page.goto('/customer/information')
  await page.getByRole('button', { name: '封筒宛名印刷', exact: true }).click()

  const dialog = page.getByRole('dialog')
  await dialog.locator('.v-select').first().click()
  await page.getByRole('option').first().click()
  await dialog.locator('.v-select').nth(1).click()
  await page.getByRole('option', { name: '角2封筒', exact: true }).click()

  const executeResponse = page.waitForResponse(response =>
    response.url().includes('/api/system/batch/execute/PRINT_ENVELOPE_KAKU2'),
  )
  await dialog.getByRole('button', { name: '宛名印刷', exact: true }).click()
  expect((await executeResponse).status()).toBe(200)

  await expect(page.getByRole('button', { name: '印刷', exact: true })).toBeVisible({
    timeout: 30_000,
  })
  await expect(page.locator('iframe.pdf-frame')).toHaveAttribute('src', /^blob:/)
})
