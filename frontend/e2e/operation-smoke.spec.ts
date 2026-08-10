import { expect, test, type Page } from '@playwright/test'

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
  await page.getByRole('button', { name: '帳票', exact: true }).click()

  await expect(page.getByText('帳票一覧', { exact: true })).toBeVisible()
  await expect(page.getByText('日別労務費一覧', { exact: true })).toBeVisible()
  await expect(page.getByText('給与支払表', { exact: true })).toBeVisible()
  await expect(page.getByText('日払い明細', { exact: true })).toBeVisible()

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
  await expect(previewFrame.getByText('E2E 給与検証社員', { exact: true })).toBeVisible()
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
  expect(generated.targetMonth).toBe('2026-08')
  expect(generated.storagePath).toContain('MONTHLY_SUMMARY/2026-08/')
  expect(sheets.length).toBeGreaterThan(0)
  expect(responseText).not.toContain('${')
  expect(responseText).toContain('E2E 月間集計検証顧客')
  expect(responseText).toContain('E2E 東京検証現場')

  const generatedDialog = page.getByRole('dialog').filter({
    hasText: '生成台帳：月間集計表',
  })
  await expect(generatedDialog).toBeVisible()
  await expect(generatedDialog.getByText('対象月: 2026-08', { exact: true })).toBeVisible()
  await expect(generatedDialog.locator('.e-spreadsheet')).toBeVisible()
  expect(serverErrors, 'same-origin HTTP 5xx responses').toEqual([])
})
