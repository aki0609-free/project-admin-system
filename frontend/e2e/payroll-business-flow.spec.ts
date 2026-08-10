import { expect, test } from '@playwright/test'
import {
  E2E_EMPLOYEE_NAME,
  E2E_TARGET_MONTH,
} from './support/business-fixture'

test('fixed employee is visible on employee information page', async ({ page }) => {
  await page.goto('/employee/information')

  await expect(page.getByRole('heading', { name: '従業員情報' })).toBeVisible()
  await expect(page.getByText(E2E_EMPLOYEE_NAME, { exact: true })).toBeVisible()
})

test('calculated daily report is visible on daily report page', async ({ page }) => {
  await page.goto('/operation/daily-reports')

  await expect(page.getByRole('heading', { name: '日報入力処理' })).toBeVisible()
  await expect(page.getByText(E2E_EMPLOYEE_NAME, { exact: true })).toBeVisible()
  await expect(page.getByText('APPROVED', { exact: true })).toBeVisible()
})

test('tracked deductions show amount, payment days and remaining days', async ({ page }) => {
  await page.goto('/operation/daily-reports')
  await page.getByText(E2E_EMPLOYEE_NAME, { exact: true }).click()

  await expect(page.getByText('日報編集', { exact: true })).toBeVisible()
  await page.getByRole('button', { name: '控除', exact: true }).click()

  await expect(page.getByText('寮費', { exact: true })).toBeVisible()
  await expect(page.getByText('携帯電話貸出料', { exact: true })).toBeVisible()
  await expect(page.getByLabel('支払い日数')).toHaveCount(2)
  await expect(page.getByText(/現在残/)).toHaveCount(2)
  await expect(page.getByText(/保存後残日数/)).toHaveCount(2)
})

test('monthly summary includes the fixed daily report', async ({ page }) => {
  await page.goto('/operation/monthly')
  await page.getByLabel('対象月').fill(E2E_TARGET_MONTH)

  await expect(page.getByRole('heading', { name: '月次管理' })).toBeVisible()
  await expect(page.getByText(E2E_TARGET_MONTH, { exact: true })).toBeVisible()
  await expect(page.getByText('日報件数', { exact: true })).toBeVisible()
  await expect(page.getByText('総支給額', { exact: true })).toBeVisible()
  await expect(page.getByText('月末支払見込', { exact: true })).toBeVisible()
})
