import { expect, test } from '@playwright/test'

const requireBox = <T>(value: T | null, label: string): T => {
  if (value == null) {
    throw new Error(`${label}の表示位置を取得できませんでした。`)
  }
  return value
}

test('dashboard notice board uses the shared toolbar, form and dialog', async ({ page }) => {
  await page.goto('/')

  await expect(page.getByText('お知らせボード', { exact: true })).toBeVisible()

  const createButton = page.getByRole('button', { name: '作成', exact: true })
  await expect(createButton).toBeVisible()
  await createButton.click()

  const dialog = page.getByRole('dialog')
  await expect(dialog.getByRole('heading', { name: 'お知らせ作成' })).toBeVisible()
  await expect(dialog.getByLabel('タイトル', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('開始日', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('終了日', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('種別', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('色', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('本文形式', { exact: true })).toBeVisible()
  await expect(dialog.getByRole('tab', { name: '編集', exact: true })).toBeVisible()
  await expect(dialog.getByRole('tab', { name: 'プレビュー', exact: true })).toBeVisible()
  await expect(dialog.getByLabel('内容', { exact: true })).toBeVisible()

  const cancelButton = dialog.getByRole('button', { name: 'キャンセル', exact: true })
  const submitButton = dialog.getByRole('button', { name: '作成', exact: true })
  const cancelBox = requireBox(await cancelButton.boundingBox(), 'キャンセルボタン')
  const submitBox = requireBox(await submitButton.boundingBox(), '作成ボタン')
  expect(cancelBox.x).toBeLessThan(submitBox.x)

  await dialog.getByRole('tab', { name: 'プレビュー', exact: true }).click()
  await expect(dialog.getByText('プレビューする内容がありません。')).toBeVisible()

  await submitButton.click()
  await expect(dialog.getByText('必須です', { exact: true })).toBeVisible()

  await cancelButton.click()
  await expect(dialog).toBeHidden()
})
