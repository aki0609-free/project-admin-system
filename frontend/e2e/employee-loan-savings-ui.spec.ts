import { expect, test } from '@playwright/test'

test('employee loan and saving dialogs use the shared form layout', async ({ page }) => {
  await page.goto('/employee/loan-savings')

  await expect(page.getByRole('heading', { name: '従業員貸付・貯蓄', exact: true })).toBeVisible()

  await page.getByRole('button', { name: '貸付 新規作成', exact: true }).click()
  const loanDialog = page.getByRole('dialog')
  await expect(
    loanDialog.getByRole('heading', { name: '従業員貸付新規作成', exact: true }),
  ).toBeVisible()
  await expect(loanDialog.getByLabel('従業員', { exact: true })).toBeVisible()
  await expect(loanDialog.getByLabel('借入元本', { exact: true })).toBeVisible()
  await expect(loanDialog.getByLabel('承認コメント', { exact: true })).toBeVisible()
  await loanDialog.getByRole('button', { name: '閉じる', exact: true }).click()

  await page.getByRole('button', { name: '貯蓄', exact: true }).click()
  await page.getByRole('button', { name: '貯蓄 新規作成', exact: true }).click()
  const savingDialog = page.getByRole('dialog')
  await expect(
    savingDialog.getByRole('heading', { name: '従業員貯蓄新規作成', exact: true }),
  ).toBeVisible()
  await expect(savingDialog.getByLabel('従業員', { exact: true })).toBeVisible()
  await expect(savingDialog.getByLabel('貯蓄率%', { exact: true })).toBeVisible()
  await expect(savingDialog.getByLabel('承認コメント', { exact: true })).toBeVisible()
  await savingDialog.getByRole('button', { name: '閉じる', exact: true }).click()
})
