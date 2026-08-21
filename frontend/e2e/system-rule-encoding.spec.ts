import { expect, test } from '@playwright/test'

const requireBox = <T>(value: T | null, label: string): T => {
  if (value == null) {
    throw new Error(`${label}の表示位置を取得できませんでした。`)
  }
  return value
}

test('dormitory daily fee rule name is displayed in Japanese', async ({ page }) => {
  await page.goto('/system/rule')

  const row = page.getByRole('row').filter({ hasText: 'DORMITORY_DAILY_FEE' })
  await expect(row).toBeVisible()
  await expect(row).toContainText('日次寮費')
  await expect(row).not.toContainText('æ—¥æ¬¡')
})

test('rule editor uses the shared dialog and role-based toolbars', async ({ page }) => {
  await page.goto('/system/rule')

  await page.getByRole('row').filter({ hasText: 'DORMITORY_DAILY_FEE' }).click()

  const dialog = page.getByRole('dialog')
  await expect(dialog.getByRole('heading', { name: 'Rule編集' })).toBeVisible()
  await expect(dialog.getByLabel('ruleName', { exact: true })).toBeDisabled()

  const cancelButton = dialog.getByRole('button', { name: 'キャンセル', exact: true })
  const updateButton = dialog.getByRole('button', { name: '更新', exact: true })
  const cancelBox = requireBox(await cancelButton.boundingBox(), 'キャンセルボタン')
  const updateBox = requireBox(await updateButton.boundingBox(), '更新ボタン')
  expect(cancelBox.x).toBeLessThan(updateBox.x)

  await dialog.getByRole('button', { name: 'Parameter', exact: true }).click()
  const parameterTab = dialog.locator('.parameter-tab')
  const addButton = parameterTab.getByRole('button', { name: '追加', exact: true })
  const deleteButton = parameterTab.getByRole('button', { name: '削除', exact: true })
  await expect(addButton).toBeVisible()
  await expect(deleteButton).toBeVisible()
  const addBox = requireBox(await addButton.boundingBox(), 'Parameter追加ボタン')
  const deleteBox = requireBox(await deleteButton.boundingBox(), 'Parameter削除ボタン')
  expect(addBox.x).toBeLessThan(deleteBox.x)

  await dialog.getByRole('button', { name: 'Test', exact: true }).click()
  const testTab = dialog.locator('.rule-test-tab')
  await expect(testTab.getByRole('button', { name: '実行', exact: true })).toBeVisible()
  await expect(testTab.getByRole('button', { name: 'クリア', exact: true })).toBeVisible()
})
