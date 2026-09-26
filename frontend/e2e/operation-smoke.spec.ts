import { expect, test, type Page } from '@playwright/test'
import {
  E2E_EMPLOYEE_NAME,
  E2E_TARGET_MONTH,
  E2E_WORK_DATE,
} from './support/business-fixture'

const watchServerErrors = (page: Page) => {
  const errors: string[] = []
  const applicationOrigin = new URL(
    process.env.E2E_BASE_URL ?? 'http://localhost:5173',
  ).origin

  page.on('response', response => {
    const url = new URL(response.url())
    if (url.origin === applicationOrigin && response.status() >= 500) {
      errors.push(`${response.status()} ${response.request().method()} ${url.pathname}`)
    }
  })

  return errors
}

test('daily operation page loads for SYS_ADMIN', async ({ page }) => {
  const serverErrors = watchServerErrors(page)

  await page.goto('/operation/daily')

  await expect(page).toHaveURL(/\/operation\/daily$/)
  await expect(page.getByRole('heading', { name: '日次管理' })).toBeVisible()
  await expect(page.getByLabel('対象日')).toBeVisible()
  expect(serverErrors, 'same-origin HTTP 5xx responses').toEqual([])
})

test('daily report HTML preview renders the fixed business data', async ({ page }) => {
  const serverErrors = watchServerErrors(page)

  await page.goto('/operation/daily')
  await page.getByLabel('対象日').click()
  const targetDate = new Date(`${E2E_WORK_DATE}T00:00:00`)
  const datePicker = page.locator('.v-date-picker')
  await expect(datePicker).toBeVisible()
  await datePicker.getByRole('button', { name: '前の月', exact: true }).click()
  await datePicker
    .getByRole('button', {
      name: new RegExp(
        `${targetDate.getFullYear()}年${targetDate.getMonth() + 1}月${targetDate.getDate()}日`,
      ),
    })
    .click()
  await page.getByRole('button', { name: '帳票', exact: true }).click()

  await expect(page.getByText('帳票一覧', { exact: true })).toBeVisible()
  await expect(page.getByText('日別労務費一覧', { exact: true })).toBeVisible()
  await expect(page.getByText('給与支払表', { exact: true })).toBeVisible()
  await expect(page.getByText('支払明細書', { exact: true })).toBeVisible()

  const previewResponsePromise = page.waitForResponse(response =>
    response.url().includes('/api/operation/report-previews/html')
    && response.url().includes('reportCode=DAILY_LABOR_COST_PREVIEW')
    && response.request().method() === 'GET',
  )
  await page.getByRole('row').filter({ hasText: 'DAILY_LABOR_COST_PREVIEW' }).click()
  const previewResponse = await previewResponsePromise
  expect(previewResponse.status(), await previewResponse.text()).toBe(200)

  const previewDialog = page.getByRole('dialog').filter({
    hasText: 'DAILY_LABOR_COST_PREVIEW',
  })
  await expect(previewDialog).toBeVisible()
  const previewFrame = previewDialog.frameLocator('iframe[title="帳票プレビュー"]')
  await expect(previewFrame.getByText('日別労務費一覧', { exact: true })).toBeVisible()
  await expect(previewFrame.getByText(E2E_EMPLOYEE_NAME, { exact: true })).toBeVisible()
  expect(serverErrors, 'same-origin HTTP 5xx responses').toEqual([])
})

test('invoice and order form are shown only in customer billing reports', async ({ page }) => {
  const serverErrors = watchServerErrors(page)

  await page.goto('/operation/monthly')
  await page.getByRole('button', { name: '帳票', exact: true }).click()
  await expect(page.getByText('MONTHLY_PAY_SLIP', { exact: true })).toBeVisible()
  await expect(page.getByText('MONTHLY_INVOICE', { exact: true })).toHaveCount(0)
  await expect(page.getByText('MONTHLY_ORDER_FORM', { exact: true })).toHaveCount(0)

  await page.goto('/operation/customer-billing')
  await page.getByRole('button', { name: '帳票一覧', exact: true }).click()
  await expect(page.getByLabel('プレビュー・印刷対象の顧客')).toHaveValue(/.+/)
  await expect(page.getByText('MONTHLY_INVOICE', { exact: true })).toBeVisible()
  await expect(page.getByText('MONTHLY_ORDER_FORM', { exact: true })).toBeVisible()

  const previewResponsePromise = page.waitForResponse(response => {
    const url = new URL(response.url())
    return url.pathname === '/api/operation/report-previews/html'
      && url.searchParams.get('reportCode') === 'MONTHLY_INVOICE'
      && Boolean(url.searchParams.get('customerId'))
      && Boolean(url.searchParams.get('periodFrom'))
      && Boolean(url.searchParams.get('periodTo'))
  })
  await page.getByRole('row').filter({ hasText: 'MONTHLY_INVOICE' }).click()
  const previewResponse = await previewResponsePromise
  expect(previewResponse.status(), await previewResponse.text()).toBe(200)
  expect(serverErrors, 'same-origin HTTP 5xx responses').toEqual([])
})

test('ledger page loads for SYS_ADMIN', async ({ page }) => {
  const serverErrors = watchServerErrors(page)

  await page.goto('/operation/book')

  await expect(page).toHaveURL(/\/operation\/book$/)
  await expect(page.getByRole('heading', { name: '台帳管理' })).toBeVisible()
  expect(serverErrors, 'same-origin HTTP 5xx responses').toEqual([])
})

test('representative spreadsheet ledger is generated and displayed', async ({ page }) => {
  const serverErrors = watchServerErrors(page)

  await page.goto('/operation/book')
  await page.locator('.v-select').filter({ hasText: '対象月' }).click()
  await page
    .getByRole('option', {
      name: `${Number(E2E_TARGET_MONTH.slice(5, 7))}月`,
      exact: true,
    })
    .click()
  const monthlySummaryRow = page.getByRole('row').filter({
    hasText: 'MONTHLY_SUMMARY',
  })
  await expect(monthlySummaryRow).toHaveCount(1)
  await expect(
    monthlySummaryRow.getByText('テンプレート', { exact: true }),
  ).toBeVisible()

  const generateResponsePromise = page.waitForResponse(response =>
    response.url().endsWith('/api/operation/excel-books/MONTHLY_SUMMARY/generate')
    && response.request().method() === 'POST',
  )
  await monthlySummaryRow.getByRole('button', { name: '生成・確認' }).click()
  const generateResponse = await generateResponsePromise
  const responseText = await generateResponse.text()
  expect(generateResponse.status(), responseText).toBe(200)

  const generated = JSON.parse(responseText) as {
    targetMonth: string
    storagePath: string
    workbook: { Workbook?: { sheets?: unknown[] }; sheets?: unknown[] }
  }
  const sheets = generated.workbook.Workbook?.sheets ?? generated.workbook.sheets ?? []
  expect(generated.targetMonth).toBe(E2E_TARGET_MONTH)
  expect(generated.storagePath).toContain(`MONTHLY_SUMMARY/${E2E_TARGET_MONTH}/`)
  expect(sheets.length).toBeGreaterThan(0)
  expect(responseText).not.toContain('${')
  expect(responseText).toContain('E2E 月間集計検証顧客')
  expect(responseText).toContain('E2E 東京検証現場')

  const generatedDialog = page.getByRole('dialog').filter({
    hasText: '生成台帳：月間集計表',
  })
  const [targetYear, targetMonthNumber] = E2E_TARGET_MONTH.split('-')
  await expect(generatedDialog).toBeVisible()
  await expect(
    generatedDialog.getByText(
      `対象月: ${targetYear}年${Number(targetMonthNumber)}月`,
      { exact: true },
    ),
  ).toBeVisible()
  await expect(generatedDialog.locator('.e-spreadsheet')).toBeVisible()
  expect(serverErrors, 'same-origin HTTP 5xx responses').toEqual([])
})
