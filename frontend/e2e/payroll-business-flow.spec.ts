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

test('daily tracked deduction shows amount, payment days and remaining days', async ({ page }) => {
  await page.goto('/operation/daily-reports')
  await page.getByText(E2E_EMPLOYEE_NAME, { exact: true }).click()

  await expect(page.getByText('日報編集', { exact: true })).toBeVisible()
  await page.getByRole('button', { name: '控除', exact: true }).click()

  const dormitoryCard = page.getByTestId('daily-report-deduction-DORMITORY_FEE')
  await expect(dormitoryCard).toBeVisible()
  await expect(dormitoryCard.getByText('寮費', { exact: true })).toBeVisible()
  await expect(page.getByText('携帯電話貸出料', { exact: true })).toHaveCount(0)
  await expect(dormitoryCard.getByLabel('支払い日数')).toHaveCount(1)
  await expect(dormitoryCard.getByText(/現在残/)).toHaveCount(1)
  await expect(dormitoryCard.getByText(/保存後残日数/)).toHaveCount(1)
})

test('daily deduction manual override and reason are stored and reopened', async ({ page }) => {
  await page.goto('/operation/daily-reports')

  const reportRow = page.getByRole('row').filter({ hasText: E2E_EMPLOYEE_NAME })
  await expect(reportRow).toHaveCount(1)
  await reportRow.click()

  let dialog = page.getByRole('dialog').filter({ hasText: '日報編集' })
  await expect(dialog).toBeVisible()
  await dialog.getByRole('button', { name: '控除', exact: true }).click()

  let dormitoryCard = dialog.getByTestId('daily-report-deduction-DORMITORY_FEE')
  await expect(dormitoryCard).toBeVisible()
  const ruleAmountText = await dormitoryCard.getByText(/Rule基準額/).innerText()
  const calculatedAmount = Number(ruleAmountText.replace(/[^0-9-]/g, ''))
  expect(calculatedAmount).toBeGreaterThan(0)

  const overrideAmount = calculatedAmount + 123
  await dormitoryCard
    .getByRole('spinbutton', { name: '金額', exact: true })
    .fill(String(overrideAmount))
  await dormitoryCard
    .getByRole('textbox', { name: '金額変更理由', exact: true })
    .fill('E2E Rule計算値の確認修正')

  const updateResponsePromise = page.waitForResponse(response =>
    /\/api\/daily-reports\/\d+$/.test(new URL(response.url()).pathname)
    && response.request().method() === 'PUT',
  )
  await dialog.getByRole('button', { name: '保存', exact: true }).click()
  const updateResponse = await updateResponsePromise
  expect(updateResponse.status(), await updateResponse.text()).toBe(200)
  await expect(dialog).toBeHidden()

  await reportRow.click()
  dialog = page.getByRole('dialog').filter({ hasText: '日報編集' })
  await expect(dialog).toBeVisible()
  await dialog.getByRole('button', { name: '控除', exact: true }).click()
  dormitoryCard = dialog.getByTestId('daily-report-deduction-DORMITORY_FEE')
  await expect(
    dormitoryCard.getByRole('spinbutton', { name: '金額', exact: true }),
  ).toHaveValue(String(overrideAmount))
  await expect(
    dormitoryCard.getByRole('textbox', { name: '金額変更理由', exact: true }),
  ).toHaveValue('E2E Rule計算値の確認修正')

  // 後続テストへ影響を残さないよう、Rule基準額へ戻して保存する。
  await dormitoryCard
    .getByRole('spinbutton', { name: '金額', exact: true })
    .fill(String(calculatedAmount))
  await expect(
    dormitoryCard.getByRole('textbox', { name: '金額変更理由', exact: true }),
  ).toHaveCount(0)
  const restoreResponsePromise = page.waitForResponse(response =>
    /\/api\/daily-reports\/\d+$/.test(new URL(response.url()).pathname)
    && response.request().method() === 'PUT',
  )
  await dialog.getByRole('button', { name: '保存', exact: true }).click()
  const restoreResponse = await restoreResponsePromise
  expect(restoreResponse.status(), await restoreResponse.text()).toBe(200)
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
