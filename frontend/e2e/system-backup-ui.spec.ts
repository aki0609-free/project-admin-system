import { expect, test } from '@playwright/test'

const requireBox = <T>(value: T | null, label: string): T => {
  if (value == null) {
    throw new Error(`${label}の表示位置を取得できませんでした。`)
  }
  return value
}

test('backup management uses the shared dialog and role-based toolbars', async ({ page }) => {
  await page.goto('/system/backup')

  await expect(page.getByRole('heading', { name: 'バックアップ' })).toBeVisible()

  const createButton = page.getByRole('button', { name: '新規追加', exact: true })
  const selectAllButton = page.getByRole('button', { name: '全選択', exact: true })
  const executeButton = page.getByRole('button', {
    name: '選択した 0 件を出力',
    exact: true,
  })

  await expect(createButton).toBeVisible()
  await expect(selectAllButton).toBeVisible()
  await expect(executeButton).toBeDisabled()

  const createBox = requireBox(await createButton.boundingBox(), '新規追加ボタン')
  const selectAllBox = requireBox(await selectAllButton.boundingBox(), '全選択ボタン')
  expect(createBox.x).toBeLessThan(selectAllBox.x)

  await createButton.click()

  const dialog = page.getByRole('dialog')
  await expect(dialog.getByRole('heading', { name: 'バックアップ対象新規作成' })).toBeVisible()
  await expect(dialog.getByLabel('対象コード', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('出力方法', { exact: true })).toBeVisible()

  await dialog.getByRole('button', { name: 'Column', exact: true }).click()

  const addButton = dialog.getByRole('button', { name: '追加', exact: true })
  const deleteButton = dialog.getByRole('button', { name: '削除', exact: true })
  await expect(addButton).toBeVisible()
  await expect(deleteButton).toBeDisabled()

  const addBox = requireBox(await addButton.boundingBox(), 'Column追加ボタン')
  const deleteBox = requireBox(await deleteButton.boundingBox(), 'Column削除ボタン')
  expect(addBox.x).toBeLessThan(deleteBox.x)

  const closeButton = dialog.getByRole('button', { name: '閉じる', exact: true })
  const saveButton = dialog.getByRole('button', { name: '保存', exact: true })
  const closeBox = requireBox(await closeButton.boundingBox(), '閉じるボタン')
  const saveBox = requireBox(await saveButton.boundingBox(), '保存ボタン')
  expect(closeBox.x).toBeLessThan(saveBox.x)

  await closeButton.click()
  await expect(dialog).toBeHidden()
})
