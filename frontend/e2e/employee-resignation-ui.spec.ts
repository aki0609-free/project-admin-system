import { expect, test } from '@playwright/test'
import { E2E_EMPLOYEE_NAME } from './support/business-fixture'

test('employee resignation dialog uses the shared form and keeps the checklist', async ({
  page,
}) => {
  await page.goto('/employee/information')

  await expect(page.getByRole('heading', { name: '従業員情報', exact: true })).toBeVisible()
  await page.getByText(E2E_EMPLOYEE_NAME, { exact: true }).click()

  const employeeDialog = page.getByRole('dialog').filter({
    has: page.getByRole('heading', { name: '従業員情報編集', exact: true }),
  })
  await expect(
    employeeDialog.getByRole('heading', { name: '従業員情報編集', exact: true }),
  ).toBeVisible()
  await employeeDialog.getByRole('button', { name: '退職', exact: true }).click()

  const resignationDialog = page.getByRole('dialog').filter({
    has: page.getByText('退職前確認チェックリスト', { exact: true }),
  })
  await expect(resignationDialog.getByLabel('退職日', { exact: true })).toBeVisible()
  await expect(
    resignationDialog.getByText('退職前確認チェックリスト', { exact: true }),
  ).toBeVisible()
  await expect(resignationDialog.getByLabel('備考', { exact: true })).toBeVisible()
  await expect(
    resignationDialog.getByRole('button', { name: '退職処理を実行', exact: true }),
  ).toBeDisabled()

  await resignationDialog.getByRole('button', { name: '閉じる', exact: true }).click()
  await expect(resignationDialog).not.toBeVisible()
  await employeeDialog.getByRole('button', { name: '閉じる', exact: true }).click()
})
