import { expect, test } from '@playwright/test'

const requireBox = <T>(value: T | null, label: string): T => {
  if (value == null) {
    throw new Error(`${label}の表示位置を取得できませんでした。`)
  }
  return value
}

test('mail management uses shared dialogs and role-based toolbars', async ({ page }) => {
  await page.goto('/system/mail')

  await expect(page.getByRole('heading', { name: 'メール管理' })).toBeVisible()

  await page.getByRole('button', { name: '新規追加', exact: true }).click()
  let dialog = page.getByRole('dialog')
  await expect(dialog.getByRole('heading', { name: '宛先グループ新規作成' })).toBeVisible()
  await expect(dialog.getByLabel('groupKey', { exact: true })).toBeVisible()

  const closeGroupButton = dialog.getByRole('button', { name: '閉じる', exact: true })
  const saveGroupButton = dialog.getByRole('button', { name: '保存', exact: true })
  const closeGroupBox = requireBox(await closeGroupButton.boundingBox(), '宛先グループの閉じるボタン')
  const saveGroupBox = requireBox(await saveGroupButton.boundingBox(), '宛先グループの保存ボタン')
  expect(closeGroupBox.x).toBeLessThan(saveGroupBox.x)

  await dialog.getByRole('button', { name: '宛先', exact: true }).click()
  const recipientEditor = dialog.locator('.mail-recipient-editor')
  const addRecipientButton = recipientEditor.getByRole('button', { name: '追加', exact: true })
  const deleteRecipientButton = recipientEditor.getByRole('button', { name: '削除', exact: true })
  await expect(addRecipientButton).toBeVisible()
  await expect(deleteRecipientButton).toBeDisabled()
  const addRecipientBox = requireBox(await addRecipientButton.boundingBox(), '宛先追加ボタン')
  const deleteRecipientBox = requireBox(await deleteRecipientButton.boundingBox(), '宛先削除ボタン')
  expect(addRecipientBox.x).toBeLessThan(deleteRecipientBox.x)

  await closeGroupButton.click()
  await expect(dialog).toBeHidden()

  await page.getByRole('button', { name: 'メッセージテンプレート', exact: true }).click()
  await page.getByRole('button', { name: '新規追加', exact: true }).click()
  dialog = page.getByRole('dialog')
  await expect(dialog.getByRole('heading', { name: 'メッセージテンプレート新規作成' })).toBeVisible()
  await expect(dialog.getByLabel('テンプレートキー', { exact: true })).toBeVisible()
  await expect(dialog.getByRole('button', { name: 'プレビュー', exact: true })).toBeDisabled()
  await expect(dialog.getByRole('button', { name: '保存', exact: true })).toBeDisabled()
  await dialog.getByRole('button', { name: '閉じる', exact: true }).click()

  await page.getByRole('button', { name: '送信キュー', exact: true }).click()
  await expect(page.getByRole('button', { name: 'WAITING送信', exact: true })).toBeVisible()

  await page.getByRole('button', { name: 'テスト送信', exact: true }).click()
  await expect(page.getByLabel('To', { exact: true })).toBeVisible()
  await expect(
    page.locator('.mail-tab').getByRole('button', { name: 'テスト送信', exact: true }),
  ).toBeVisible()
})
