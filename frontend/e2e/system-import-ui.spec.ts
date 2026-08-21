import { expect, test } from '@playwright/test'

const requireBox = <T>(value: T | null, label: string): T => {
  if (value == null) {
    throw new Error(`${label}の表示位置を取得できませんでした。`)
  }
  return value
}

test('external import uses shared dialog and role-based toolbars', async ({ page }) => {
  await page.goto('/system/import')

  await expect(page.getByRole('heading', { name: '外部データ取込' })).toBeVisible()

  const createButton = page.getByRole('button', { name: '新規追加', exact: true })
  await expect(createButton).toBeVisible()
  await createButton.click()

  const dialog = page.getByRole('dialog')
  await expect(dialog.getByRole('heading', { name: 'インポート定義新規作成' })).toBeVisible()
  await expect(dialog.getByLabel('targetCode', { exact: true })).toBeVisible()
  await expect(dialog.getByLabel('取込先テーブル', { exact: true })).toBeVisible()

  const closeButton = dialog.getByRole('button', { name: '閉じる', exact: true })
  const saveButton = dialog.getByRole('button', { name: '保存', exact: true })
  const closeBox = requireBox(await closeButton.boundingBox(), '閉じるボタン')
  const saveBox = requireBox(await saveButton.boundingBox(), '保存ボタン')
  expect(closeBox.x).toBeLessThan(saveBox.x)

  await dialog.getByRole('button', { name: 'Column', exact: true }).click()

  const addButton = dialog.getByRole('button', { name: '追加', exact: true })
  const deleteButton = dialog.getByRole('button', { name: '削除', exact: true })
  await expect(addButton).toBeVisible()
  await expect(deleteButton).toBeDisabled()

  const addBox = requireBox(await addButton.boundingBox(), 'Column追加ボタン')
  const deleteBox = requireBox(await deleteButton.boundingBox(), 'Column削除ボタン')
  expect(addBox.x).toBeLessThan(deleteBox.x)

  await addButton.click()
  await expect(dialog.getByText('選択中Column', { exact: true })).toBeVisible()
  await expect(deleteButton).toBeEnabled()

  await closeButton.click()
  await expect(dialog).toBeHidden()

  await page.getByRole('button', { name: 'インポート実行', exact: true }).click()
  await expect(page.getByText('取込定義を選択して、CSV取込を実行します。')).toBeVisible()

  await page.getByRole('button', { name: '履歴', exact: true }).click()
  await expect(page.getByText('executedAt', { exact: true }).first()).toBeVisible()
})

test('import history opens error rows in the shared dialog', async ({ page }) => {
  await page.route('**/api/system/import-history', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {
          id: 901,
          targetCode: 'EMPLOYEE_TEST',
          targetName: '従業員テスト取込',
          tableName: 'employees',
          sourceType: 'UPLOAD',
          importMode: 'UPSERT',
          fileName: 'employee-test.csv',
          totalCount: 1,
          insertedCount: 0,
          updatedCount: 0,
          skippedCount: 0,
          errorCount: 1,
          status: 'FAILED',
          jobExecutionId: null,
          executedBy: 'e2e-user',
          executedAt: '2026-08-21T10:00:00',
          errorMessage: '入力値を確認してください。',
        },
      ]),
    })
  })

  await page.route('**/api/system/import-history/901/errors', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {
          id: 902,
          rowNo: 2,
          csvHeaderName: '社員コード',
          columnName: 'employee_code',
          rawValue: '',
          errorMessage: '社員コードは必須です。',
        },
      ]),
    })
  })

  await page.goto('/system/import')
  await page.getByRole('button', { name: '履歴', exact: true }).click()
  await page.getByText('EMPLOYEE_TEST', { exact: true }).click()

  const dialog = page.getByRole('dialog')
  await expect(dialog.getByRole('heading', { name: 'インポートエラー行' })).toBeVisible()
  await expect(dialog.getByText('社員コードは必須です。', { exact: true })).toBeVisible()

  await dialog.getByRole('button', { name: '閉じる', exact: true }).click()
  await expect(dialog).toBeHidden()
})
