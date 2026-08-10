import { expect, test, type APIResponse, type Page } from '@playwright/test'
import {
  E2E_EMPLOYEE_CODE,
  E2E_TARGET_MONTH,
  E2E_WORK_DATE,
} from './support/business-fixture'

type BatchExecuteResponse = {
  status: 'STARTED' | 'COMPLETED' | 'FAILED'
  message: string
  logId: number
  outputFileKey?: string | null
  outputFileName?: string | null
  contentType?: string | null
  fileSize?: number | null
}

type SelectionOption = {
  value: string
  displayValues: Record<string, unknown>
}

type SelectionResponse = {
  mode: 'NONE' | 'SINGLE' | 'MULTIPLE'
  allowSelectAll: boolean
  generationUnit: 'ONE_FILE' | 'FILE_PER_SELECTION'
  options: SelectionOption[]
}

type GeneratedLedger = {
  bookCode: string
  targetMonth: string
  storagePath: string
  workbookBytes: number
  rowCount: number
  selectionValue: string | null
  workbook: unknown
}

type EmployeeListItem = {
  id: number
  employeeCode: string
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

const executeBatch = async (
  page: Page,
  jobCode: string,
  params: Record<string, unknown>,
) => {
  const response = await page.request.post(
    `/api/system/batch/execute/${encodeURIComponent(jobCode)}`,
    {
      headers: await authenticatedHeaders(page),
      data: { params },
    },
  )
  const result = await responseJson<BatchExecuteResponse>(response)
  expect(result.status, result.message).toBe('COMPLETED')
  expect(result.logId).toBeGreaterThan(0)
  expect(result.outputFileKey).toBeTruthy()
  expect(result.outputFileName).toBeTruthy()
  expect(result.fileSize ?? 0).toBeGreaterThan(0)
  return result
}

const downloadBatchFile = async (page: Page, logId: number) => {
  return page.request.get(`/api/system/batch/logs/${logId}/file`, {
    headers: await authenticatedHeaders(page),
  })
}

test.beforeEach(async ({ page }) => {
  await page.goto('/')
})

test('employee toolbar exposes CSV export and individual daily pay slip parameters', async ({ page }) => {
  await page.goto('/employee/information')

  await expect(page.getByRole('button', { name: '個別日別給与明細' })).toBeVisible()
  await expect(page.getByRole('button', { name: '従業員CSV出力' })).toBeVisible()

  await page.getByRole('button', { name: '個別日別給与明細' }).click()
  const paySlipDialog = page.getByRole('dialog')
  await expect(paySlipDialog.getByText('個別日別給与明細', { exact: true })).toBeVisible()
  await expect(paySlipDialog.getByRole('textbox', { name: '支払日' })).toBeVisible()
  await expect(paySlipDialog.getByRole('combobox', { name: '従業員' })).toBeVisible()
  await paySlipDialog.getByRole('button', { name: '閉じる' }).click()

  await page.getByRole('button', { name: '従業員CSV出力' }).click()
  const csvDialog = page.getByRole('dialog')
  await expect(csvDialog.getByText('従業員CSV出力', { exact: true })).toBeVisible()
  await expect(csvDialog.getByRole('checkbox', { name: '削除済みを含める' })).toBeVisible()
})

test('individual daily pay slip is stored and downloaded as a valid PDF', async ({ page }) => {
  const employeeResponse = await page.request.get('/api/employees', {
    headers: await authenticatedHeaders(page),
  })
  const employees = await responseJson<EmployeeListItem[]>(employeeResponse)
  const employee = employees.find(item => item.employeeCode === E2E_EMPLOYEE_CODE)
  expect(employee, `${E2E_EMPLOYEE_CODE} employee`).toBeDefined()
  if (!employee) {
    throw new Error(`${E2E_EMPLOYEE_CODE} employee was not found`)
  }

  const result = await executeBatch(page, 'PRINT_DAILY_PAY_SLIP', {
    paymentDate: E2E_WORK_DATE,
    employeeId: employee.id,
  })

  expect(result.outputFileName).toMatch(/\.pdf$/i)
  expect(result.contentType).toBe('application/pdf')

  const download = await downloadBatchFile(page, result.logId)
  expect(download.status(), await download.text()).toBe(200)
  expect(download.headers()['content-type']).toContain('application/pdf')
  expect(download.headers()['content-disposition']).toContain('filename')

  const file = await download.body()
  expect(file.subarray(0, 5).toString('ascii')).toBe('%PDF-')
  expect(file.length).toBe(result.fileSize)
})

test('employee CSV is stored and downloaded with UTF-8 BOM', async ({ page }) => {
  const result = await executeBatch(page, 'EXPORT_EMPLOYEE_CSV', {
    includeDeleted: false,
  })

  expect(result.outputFileName).toMatch(/\.csv$/i)
  expect(result.contentType).toContain('text/csv')

  const download = await downloadBatchFile(page, result.logId)
  expect(download.status(), await download.text()).toBe(200)
  expect(download.headers()['content-type']).toContain('text/csv')

  const file = await download.body()
  expect([...file.subarray(0, 3)]).toEqual([0xef, 0xbb, 0xbf])
  const csv = file.subarray(3).toString('utf8')
  expect(csv).toContain('社員コード')
  expect(csv).toContain(E2E_EMPLOYEE_CODE)
  expect(file.length).toBe(result.fileSize)
})

test('monthly labor cost list is stored and downloaded as a valid XLSX', async ({ page }) => {
  const result = await executeBatch(page, 'PRINT_MONTHLY_LABOR_COST_LIST', {
    targetMonth: E2E_TARGET_MONTH,
    closingVersion: 900001,
    executionMode: 'RETRY',
  })

  expect(result.outputFileName).toMatch(/\.xlsx$/i)
  expect(result.contentType).toContain('spreadsheetml.sheet')

  const download = await downloadBatchFile(page, result.logId)
  expect(download.status(), await download.text()).toBe(200)
  expect(download.headers()['content-type']).toContain('spreadsheetml.sheet')

  const file = await download.body()
  expect(file.subarray(0, 2).toString('ascii')).toBe('PK')
  expect(file.length).toBe(result.fileSize)
})

test('monthly labor ledger supports individual and select-all generation', async ({ page }) => {
  const headers = await authenticatedHeaders(page)
  const selectionResponse = await page.request.get(
    `/api/operation/excel-books/MONTHLY_LABOR/selection-options?targetMonth=${E2E_TARGET_MONTH}`,
    { headers },
  )
  const selection = await responseJson<SelectionResponse>(selectionResponse)

  expect(selection.mode).toBe('MULTIPLE')
  expect(selection.allowSelectAll).toBe(true)
  expect(selection.generationUnit).toBe('FILE_PER_SELECTION')
  expect(selection.options.length).toBeGreaterThan(0)

  const employeeOption = selection.options.find(option =>
    Object.values(option.displayValues).some(value => value === E2E_EMPLOYEE_CODE),
  )
  expect(employeeOption, `${E2E_EMPLOYEE_CODE} selection option`).toBeDefined()
  if (!employeeOption) {
    throw new Error(`${E2E_EMPLOYEE_CODE} selection option was not found`)
  }

  const individualResponse = await page.request.post(
    '/api/operation/excel-books/MONTHLY_LABOR/generate-selected',
    {
      headers,
      data: {
        targetMonth: E2E_TARGET_MONTH,
        selectionValues: [employeeOption.value],
      },
    },
  )
  const individual = await responseJson<GeneratedLedger[]>(individualResponse)
  expect(individual).toHaveLength(1)
  expect(individual[0]?.selectionValue).toBe(employeeOption.value)
  expect(individual[0]?.storagePath).toContain(`MONTHLY_LABOR/${E2E_TARGET_MONTH}/`)
  expect(individual[0]?.workbookBytes ?? 0).toBeGreaterThan(0)
  expect(JSON.stringify(individual[0]?.workbook)).not.toContain('${')

  const allValues = selection.options.map(option => option.value)
  const allResponse = await page.request.post(
    '/api/operation/excel-books/MONTHLY_LABOR/generate-selected',
    {
      headers,
      data: {
        targetMonth: E2E_TARGET_MONTH,
        selectionValues: allValues,
      },
    },
  )
  const generatedAll = await responseJson<GeneratedLedger[]>(allResponse)
  expect(generatedAll).toHaveLength(allValues.length)
  expect(generatedAll.map(item => item.selectionValue).sort())
    .toEqual([...allValues].sort())
  for (const generated of generatedAll) {
    expect(generated.bookCode).toBe('MONTHLY_LABOR')
    expect(generated.targetMonth).toBe(E2E_TARGET_MONTH)
    expect(generated.storagePath).toContain(`MONTHLY_LABOR/${E2E_TARGET_MONTH}/`)
    expect(generated.workbookBytes).toBeGreaterThan(0)
  }
})
