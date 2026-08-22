import { expect, test } from '@playwright/test'

const requireBox = <T>(value: T | null, label: string): T => {
  if (value == null) {
    throw new Error(`${label}の表示位置を取得できませんでした。`)
  }
  return value
}

test('user management uses the shared page, table, form and dialog', async ({ page }) => {
  await page.goto('/user/users')

  await expect(page.getByRole('heading', { name: 'ユーザー管理' })).toBeVisible()
  await page.getByRole('button', { name: '新規追加', exact: true }).click()

  const dialog = page.getByRole('dialog')
  await expect(dialog.getByRole('heading', { name: 'ユーザー新規作成' })).toBeVisible()
  await expect(dialog.getByLabel('ユーザー名', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('パスワード', { exact: true })).toBeVisible()
  await expect(dialog.getByText('ロール', { exact: true }).last()).toBeVisible()

  await dialog.getByRole('button', { name: '保存', exact: true }).click()
  await expect(dialog.getByText('ユーザー名は必須です')).toBeVisible()
  await expect(dialog.getByText('パスワードは必須です')).toBeVisible()
  await expect(dialog).toBeVisible()

  const cancelButton = dialog.getByRole('button', { name: 'キャンセル', exact: true })
  const saveButton = dialog.getByRole('button', { name: '保存', exact: true })
  const cancelBox = requireBox(await cancelButton.boundingBox(), 'キャンセルボタン')
  const saveBox = requireBox(await saveButton.boundingBox(), '保存ボタン')
  expect(cancelBox.x).toBeLessThan(saveBox.x)

  await cancelButton.click()
  await expect(dialog).toBeHidden()
})

test('role management uses the shared page, table, form and dialog', async ({ page }) => {
  await page.goto('/user/auth')

  await expect(page.getByRole('heading', { name: '権限管理' })).toBeVisible()
  await page.getByRole('button', { name: '新規追加', exact: true }).click()

  const dialog = page.getByRole('dialog')
  await expect(dialog.getByRole('heading', { name: 'ロール新規作成' })).toBeVisible()
  await expect(dialog.getByLabel('ロール名', { exact: true })).toBeVisible()
  await expect(dialog.getByText('選択中の権限', { exact: true })).toBeVisible()

  await dialog.getByRole('button', { name: '保存', exact: true }).click()
  await expect(dialog.getByText('ロール名は必須です')).toBeVisible()
  await expect(dialog.getByText('権限を1つ以上選択してください')).toBeVisible()
  await expect(dialog).toBeVisible()

  const cancelButton = dialog.getByRole('button', { name: 'キャンセル', exact: true })
  const saveButton = dialog.getByRole('button', { name: '保存', exact: true })
  const cancelBox = requireBox(await cancelButton.boundingBox(), 'キャンセルボタン')
  const saveBox = requireBox(await saveButton.boundingBox(), '保存ボタン')
  expect(cancelBox.x).toBeLessThan(saveBox.x)

  await cancelButton.click()
  await expect(dialog).toBeHidden()
})
