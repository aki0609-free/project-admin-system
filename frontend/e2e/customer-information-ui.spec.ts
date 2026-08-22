import { expect, test } from '@playwright/test'

import { E2E_CUSTOMER_NAME, E2E_SITE_NAME } from './support/business-fixture'

test('customer information opens detail tabs and validates a new customer', async ({ page }) => {
  await page.goto('/customer/information')

  await expect(page.getByRole('heading', { name: '顧客管理', exact: true })).toBeVisible()
  await page.getByText(E2E_CUSTOMER_NAME, { exact: true }).click()

  let dialog = page.getByRole('dialog')
  await expect(dialog.getByText('顧客 編集', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('顧客名', { exact: true })).toHaveValue(E2E_CUSTOMER_NAME)
  await dialog.getByText('現場一覧', { exact: true }).click()
  await expect(dialog.getByText(E2E_SITE_NAME, { exact: true })).toBeVisible()

  await dialog.getByRole('button', { name: '現場追加', exact: true }).click()
  let addedRow = dialog.locator('tbody tr').last()
  await expect(addedRow.locator('td').nth(1)).toHaveText('')
  await addedRow.locator('td').last().click()
  await addedRow.getByRole('spinbutton').fill('12')
  await addedRow.locator('td').nth(2).click()
  await expect(addedRow.locator('td').last()).toHaveText('12km')

  await dialog.getByRole('button', { name: '顧客社員', exact: true }).click()
  await dialog.getByRole('button', { name: '顧客社員追加', exact: true }).click()
  addedRow = dialog.locator('tbody tr').last()
  await expect(addedRow.locator('td').nth(1)).toHaveText('')

  await dialog.getByRole('button', { name: '請求単価', exact: true }).click()
  await dialog.getByRole('button', { name: '単価追加', exact: true }).click()
  addedRow = dialog.locator('tbody tr').last()
  await addedRow.locator('td').nth(8).click()
  await addedRow.getByRole('spinbutton').fill('12345')
  await addedRow.locator('td').nth(9).click()
  await expect(addedRow.locator('td').nth(8)).toHaveText('12,345円')

  await dialog.getByRole('button', { name: 'キャンセル', exact: true }).click()

  await page.getByRole('button', { name: '新規登録', exact: true }).click()
  dialog = page.getByRole('dialog')
  await dialog.getByRole('button', { name: '顧客情報を保存', exact: true }).click()
  await expect(dialog.getByText('顧客名は必須です', { exact: true }).first()).toBeVisible()
})
