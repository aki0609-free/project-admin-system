import { expect, test } from '@playwright/test'

test('business settings uses shared forms and checklist dialog', async ({ page }) => {
  await page.goto('/admin/business-settings')

  await expect(page.getByRole('heading', { name: '業務管理' })).toBeVisible()
  await expect(page.getByLabel('ダイアログタイトル', { exact: true })).toBeVisible()
  await expect(page.getByLabel('案内文', { exact: true })).toBeVisible()
  await expect(page.getByLabel('警告見出し', { exact: true })).toBeVisible()

  await page.getByRole('button', { name: 'TODO追加', exact: true }).click()
  const dialog = page.getByRole('dialog')
  await expect(dialog.getByRole('heading', { name: '退職TODO追加' })).toBeVisible()
  await expect(dialog.getByLabel('TODOコード', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('項目名', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('説明', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('表示順', { exact: true })).toBeVisible()
  await expect(dialog.getByRole('button', { name: '閉じる', exact: true })).toBeVisible()
  await expect(dialog.getByRole('button', { name: '保存', exact: true })).toBeVisible()
  await dialog.getByRole('button', { name: '閉じる', exact: true }).click()

  await page.getByRole('tab', { name: '締日設定', exact: true }).click()
  await expect(page.getByText('給与の締日・支払日', { exact: true })).toBeVisible()

  await page.getByRole('tab', { name: '締め帳票', exact: true }).click()
  await expect(page.getByText('月次締め帳票', { exact: true })).toBeVisible()

  await page.getByRole('tab', { name: '帳票バックアップ', exact: true }).click()
  await expect(page.getByLabel('会計年度の開始月', { exact: true })).toBeVisible()
  await expect(page.getByLabel('年度終了後の猶予日数', { exact: true })).toBeVisible()

  await page.getByRole('tab', { name: 'その他設定', exact: true }).click()
  await expect(page.getByLabel('インシデント報告のURL', { exact: true })).toBeVisible()
  await expect(page.getByLabel('マニュアルのURL', { exact: true })).toBeVisible()

  const incidentUrl = page.getByLabel('インシデント報告のURL', { exact: true })
  await incidentUrl.fill('http://example.invalid/incident')
  await incidentUrl.blur()
  await expect(page.getByText('HTTPSのURLを入力してください', { exact: true })).toBeVisible()
})
