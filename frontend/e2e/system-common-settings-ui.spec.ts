import { expect, test, type Page } from '@playwright/test'

const selectToolbarOption = async (
  page: Page,
  label: string,
  option: string,
) => {
  const combobox = page.getByRole('combobox', { name: label }).first()
  await combobox.focus()
  await combobox.press('ArrowDown')
  await page.getByRole('option', { name: option, exact: true }).click()
}

test('standard setting values are edited from the setting value master', async ({ page }) => {
  await page.goto('/system/common-settings')

  await expect(page.getByRole('heading', { name: 'システム共通設定' })).toBeVisible()
  await expect(page.getByRole('tab', { name: '設定値マスター' })).toBeVisible()
  await expect(page.getByRole('combobox', { name: '大分類' }).first()).toBeVisible()
  await expect(page.getByRole('combobox', { name: '中分類' }).first()).toBeVisible()
  await expect(page.getByRole('combobox', { name: '小分類' }).first()).toBeVisible()
  await expect(page.getByRole('row').filter({ hasText: 'ダイアログタイトル' })).toBeVisible()
  await expect(page.getByRole('row').filter({ hasText: '案内文' })).toBeVisible()
  await expect(page.getByRole('row').filter({ hasText: '警告見出し' })).toBeVisible()

  const row = page.getByRole('row').filter({ hasText: 'インシデント報告URL' })
  await expect(row).toBeVisible()
  await row.click()

  await expect(page.getByText('標準項目の定義は保護されています。')).toBeVisible()
  const settingValue = page.getByRole('dialog').getByLabel('設定値', { exact: true })
  await settingValue.fill('http://example.invalid/incident')
  await page.getByRole('button', { name: '保存', exact: true }).click()
  await expect(page.getByText(
    'インシデント報告URLには有効なHTTPS URLを指定してください。',
    { exact: true },
  )).toBeVisible()
})

test('tenant setting definitions and values can be created edited filtered and deleted', async ({ page }) => {
  await page.goto('/system/common-settings')

  const dialog = page.getByRole('dialog')
  await expect(page.getByRole('row').filter({ hasText: 'マニュアルURL' })).toBeVisible()
  const existingRow = page.getByRole('row').filter({ hasText: /E2E通知先/ })
  if (await existingRow.count()) {
    await existingRow.click()
    page.once('dialog', confirmation => confirmation.accept())
    await dialog.getByRole('button', { name: '削除', exact: true }).click()
    await expect(dialog).not.toBeVisible()
  }

  await page.getByRole('button', { name: '設定項目を追加' }).click()

  await dialog.getByLabel('大分類キー').fill('E2E_CUSTOM')
  await dialog.getByLabel('大分類名').fill('E2E会社独自設定')
  await dialog.getByLabel('中分類キー').fill('NOTIFICATION')
  await dialog.getByLabel('中分類名').fill('通知')
  await dialog.getByLabel('小分類キー').fill('DESTINATION')
  await dialog.getByLabel('設定項目名').fill('E2E通知先')
  await dialog.getByLabel('設定値', { exact: true }).fill('initial@example.invalid')
  await dialog.getByRole('button', { name: '保存', exact: true }).click()
  await expect(dialog).not.toBeVisible()
  await page.waitForTimeout(500)

  await selectToolbarOption(page, '大分類', 'E2E会社独自設定')
  await selectToolbarOption(page, '中分類', '通知')
  await selectToolbarOption(page, '小分類', 'E2E通知先')

  let row = page.getByRole('row').filter({ hasText: 'E2E通知先' })
  await expect(row).toContainText('initial@example.invalid')
  await row.click()
  await dialog.getByLabel('設定項目名').fill('E2E通知先（更新）')
  await dialog.getByLabel('設定値', { exact: true }).fill('updated@example.invalid')
  await dialog.getByRole('button', { name: '保存', exact: true }).click()
  await expect(dialog).not.toBeVisible()
  await page.waitForTimeout(500)

  await page.getByRole('button', { name: '条件クリア' }).click()
  row = page.getByRole('row').filter({ hasText: 'E2E通知先（更新）' })
  await expect(row).toContainText('updated@example.invalid')
  await row.click()
  page.once('dialog', dialog => dialog.accept())
  await dialog.getByRole('button', { name: '削除', exact: true }).click()
  await expect(row).not.toBeVisible()
})
