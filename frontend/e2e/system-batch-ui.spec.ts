import { expect, test } from '@playwright/test'

test('batch management uses shared dialogs without changing execution data', async ({ page }) => {
  await page.goto('/system/batch')

  await expect(page.getByRole('heading', { name: 'バッチ管理' })).toBeVisible()

  const createButton = page.getByRole('button', { name: '新規作成', exact: true })
  await expect(createButton).toBeVisible()
  await createButton.click()

  let definitionDialog = page.getByRole('dialog').filter({ hasText: 'Batch定義新規作成' })
  await expect(definitionDialog).toBeVisible()
  await expect(definitionDialog.getByLabel('jobCode', { exact: true })).toBeVisible()
  await expect(definitionDialog.getByLabel('説明', { exact: true })).toBeVisible()
  await definitionDialog.getByRole('button', { name: '閉じる', exact: true }).click()
  await expect(definitionDialog).toBeHidden()

  const existingRow = page.getByRole('row').filter({ hasText: 'PRINT_MONTHLY_LABOR_COST_LIST' })
  await expect(existingRow).toHaveCount(1)
  await existingRow.click()

  definitionDialog = page.getByRole('dialog').filter({ hasText: 'Batch定義編集' })
  await expect(definitionDialog).toBeVisible()
  await expect(
    definitionDialog.getByRole('button', { name: '即時実行', exact: true }),
  ).toBeEnabled()
  await definitionDialog.getByRole('button', { name: '即時実行', exact: true }).click()

  const executeDialog = page.getByRole('dialog').filter({ hasText: 'バッチ実行' })
  await expect(executeDialog).toBeVisible()
  await expect(executeDialog.locator('.summary')).toContainText('PRINT_MONTHLY_LABOR_COST_LIST')

  await executeDialog.getByLabel('Params JSON', { exact: true }).fill('[]')
  await executeDialog.getByRole('button', { name: '実行', exact: true }).click()
  await expect(executeDialog.getByText('JSONはオブジェクト形式で入力してください。')).toBeVisible()

  await executeDialog.getByRole('button', { name: '閉じる', exact: true }).click()
  await expect(executeDialog).toBeHidden()

  await definitionDialog.getByRole('button', { name: '即時実行', exact: true }).click()
  await expect(executeDialog.getByLabel('Params JSON', { exact: true })).toHaveValue('{}')
  await expect(
    executeDialog.getByText('JSONはオブジェクト形式で入力してください。'),
  ).toHaveCount(0)

  await executeDialog.getByRole('button', { name: '閉じる', exact: true }).click()
  await definitionDialog.getByRole('button', { name: '閉じる', exact: true }).click()
})
