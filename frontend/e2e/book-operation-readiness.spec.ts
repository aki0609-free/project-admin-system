import { expect, test, type APIResponse, type Page } from '@playwright/test'

type OperationBook = {
  bookCode: string
  generationReady: boolean
  monthlyClosingConfigured: boolean
  readinessIssues: string[]
}

type OperationBookSettings = {
  fiscalYearStartMonth: number
}

const responseJson = async <T>(response: APIResponse): Promise<T> => {
  const body = await response.text()
  expect(response.ok(), `${response.status()} ${response.url()}\n${body}`).toBeTruthy()
  return JSON.parse(body) as T
}

const authenticatedHeaders = async (page: Page) => {
  const accessToken = await page.evaluate(() => localStorage.getItem('accessToken'))
  expect(accessToken, 'authenticated access token').not.toBeNull()
  return {
    Authorization: `Bearer ${accessToken}`,
    'X-Tenant-ID': 'default',
  }
}

test('system ledger masters expose generation readiness and monthly closing link', async ({ page }) => {
  await page.goto('/')
  const headers = await authenticatedHeaders(page)

  const settings = await responseJson<OperationBookSettings>(
    await page.request.get('/api/operation/excel-books/settings', { headers }),
  )
  expect(settings.fiscalYearStartMonth).toBeGreaterThanOrEqual(1)
  expect(settings.fiscalYearStartMonth).toBeLessThanOrEqual(12)

  const books = await responseJson<OperationBook[]>(
    await page.request.get('/api/operation/excel-books', { headers }),
  )
  for (const code of [
    'MONTHLY_LABOR',
    'LABOR_COST_PAYMENT',
    'RECEIPT_CONFIRMATION',
    'MONTHLY_SUMMARY',
  ]) {
    const book = books.find(item => item.bookCode === code)
    expect(book, `${code} system master`).toBeDefined()
    expect(book?.generationReady, book?.readinessIssues.join(' / ')).toBe(true)
    expect(book?.monthlyClosingConfigured, `${code} closing definition`).toBe(true)
  }

  await page.goto('/operation/book')
  await expect(page.getByRole('heading', { name: '台帳管理' })).toBeVisible()
  await expect(page.getByRole('columnheader', { name: '月次締め' })).toBeVisible()
  await expect(page.getByText('締め対象', { exact: true })).toHaveCount(4)
})
