import { expect, test } from '@playwright/test'

const requireBox = <T>(value: T | null, label: string): T => {
  if (value == null) {
    throw new Error(`${label}の表示位置を取得できませんでした。`)
  }
  return value
}

test('notice rule management uses shared dialog and role-based toolbar', async ({ page }) => {
  await page.goto('/system/notice')

  await expect(page.getByRole('heading', { name: 'お知らせルール管理' })).toBeVisible()

  const createButton = page.getByRole('button', { name: '新規追加', exact: true })
  const generateButton = page.getByRole('button', { name: '手動生成', exact: true })
  const reloadButton = page.getByRole('button', { name: 'スケジュール再読込', exact: true })

  await expect(createButton).toBeVisible()
  await expect(generateButton).toBeVisible()
  await expect(reloadButton).toBeVisible()

  const createBox = requireBox(await createButton.boundingBox(), '新規追加ボタン')
  const generateBox = requireBox(await generateButton.boundingBox(), '手動生成ボタン')
  expect(createBox.x).toBeLessThan(generateBox.x)

  await createButton.click()

  const dialog = page.getByRole('dialog')
  await expect(dialog.getByRole('heading', { name: 'お知らせルール新規作成' })).toBeVisible()
  await expect(dialog.getByLabel('ruleCode', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('ルール名', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('cron', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('タイトルテンプレート', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('本文形式', { exact: true })).toBeVisible()
  await expect(dialog.getByText('使用可能な変数：{label} / {date} / {key}')).toBeVisible()

  const closeButton = dialog.getByRole('button', { name: '閉じる', exact: true })
  const saveButton = dialog.getByRole('button', { name: '保存', exact: true })
  const closeBox = requireBox(await closeButton.boundingBox(), '閉じるボタン')
  const saveBox = requireBox(await saveButton.boundingBox(), '保存ボタン')
  expect(closeBox.x).toBeLessThan(saveBox.x)

  await closeButton.click()
  await expect(dialog).toBeHidden()
})
