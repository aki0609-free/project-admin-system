import { expect, test } from '@playwright/test'

const employeeEnrollmentDeductions = [
  { code: 'DORMITORY_FEE', inputSource: '日報' },
  { code: 'MOBILE_RENTAL', inputSource: '明細取引' },
  { code: 'WIFI_FEE', inputSource: '明細取引' },
] as const

test('employee payroll item tabs are backed by the deduction master policy', async ({ page }) => {
  await page.goto('/master/deduction')

  for (const item of employeeEnrollmentDeductions) {
    await page.getByText(item.code, { exact: true }).click()
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible()
    await dialog.getByRole('button', { name: '適用・連携設定', exact: true }).click()

    await expect(dialog.locator('.v-select').filter({ hasText: '適用対象' })).toContainText(
      '従業員ごとに設定',
    )
    await expect(dialog.locator('.v-select').filter({ hasText: '標準入力元' })).toContainText(
      item.inputSource,
    )

    await dialog.getByRole('button', { name: 'キャンセル', exact: true }).click()
    await expect(dialog).not.toBeVisible()
  }
})
