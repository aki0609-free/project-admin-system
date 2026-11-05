import { expect, test } from '@playwright/test'

test('published generic master appears as a business settings tab and manages records', async ({ page }) => {
  const suffix = Date.now().toString()
  const masterCode = `E2E_DYNAMIC_${suffix}`
  const masterName = `E2E動的マスター${suffix}`
  const tabName = `E2E設定${suffix}`

  await page.goto('/system/common-settings')
  await page.getByRole('tab', { name: '汎用マスター定義' }).click()
  await page.getByRole('button', { name: '汎用マスターを追加' }).click()

  let dialog = page.getByRole('dialog')
  await dialog.getByLabel('マスターコード').fill(masterCode)
  await dialog.getByLabel('マスター名').fill(masterName)
  await dialog.getByLabel('業務管理タブ名').fill(tabName)
  await dialog.getByLabel('項目キー').fill('DISPLAY_NAME')
  await dialog.getByLabel('項目名').fill('表示名称')
  await dialog.getByRole('button', { name: '保存', exact: true }).click()
  await expect(dialog).not.toBeVisible()

  const definitionRow = page.getByRole('row').filter({ hasText: masterCode })
  await expect(definitionRow).toContainText('下書き')
  await definitionRow.click()
  page.once('dialog', confirmation => confirmation.accept())
  await dialog.getByRole('button', { name: '公開', exact: true }).click()
  await expect(dialog).not.toBeVisible()

  await page.goto('/admin/business-settings')
  await page.getByRole('tab', { name: tabName }).click()
  await expect(page.getByRole('heading', { name: masterName })).toBeVisible()
  await page.getByRole('button', { name: `${tabName}を追加` }).click()

  dialog = page.getByRole('dialog')
  await dialog.getByLabel('表示名称').fill('動的入力テスト')
  await dialog.getByRole('button', { name: '保存', exact: true }).click()
  await expect(dialog).not.toBeVisible()

  const dataRow = page.getByRole('row').filter({ hasText: '動的入力テスト' })
  await expect(dataRow).toBeVisible()
  await dataRow.click()
  page.once('dialog', confirmation => confirmation.accept())
  await dialog.getByRole('button', { name: '削除', exact: true }).click()
  await expect(dataRow).not.toBeVisible()

  await page.goto('/system/common-settings')
  await page.getByRole('tab', { name: '汎用マスター定義' }).click()
  const cleanupRow = page.getByRole('row').filter({ hasText: masterCode })
  await cleanupRow.click()
  page.once('dialog', confirmation => confirmation.accept())
  await page.getByRole('dialog').getByRole('button', { name: '削除', exact: true }).click()
  await expect(cleanupRow).not.toBeVisible()
})
