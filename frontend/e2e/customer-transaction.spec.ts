import { expect, test, type Page } from '@playwright/test'

const CUSTOMER_PREFIX = 'E2E画面顧客-'

type CustomerListItem = {
  id: number
  name: string
}

type CustomerTransaction = {
  id: number
}

const authenticatedHeaders = async (page: Page) => {
  const accessToken = await page.evaluate(() => localStorage.getItem('accessToken'))
  expect(accessToken, 'authenticated access token').not.toBeNull()

  return {
    Authorization: `Bearer ${accessToken}`,
    'X-Tenant-ID': 'default',
  }
}

const cleanupCustomers = async (page: Page) => {
  const headers = await authenticatedHeaders(page)
  const customersResponse = await page.request.get('/api/customers', { headers })
  expect(customersResponse.ok(), await customersResponse.text()).toBeTruthy()
  const customers = await customersResponse.json() as CustomerListItem[]

  for (const customer of customers.filter(item => item.name.startsWith(CUSTOMER_PREFIX))) {
    const transactionsResponse = await page.request.get(
      `/api/customers/${customer.id}/transactions`,
      { headers },
    )
    expect(transactionsResponse.ok(), await transactionsResponse.text()).toBeTruthy()
    const transactions = await transactionsResponse.json() as CustomerTransaction[]

    for (const transaction of transactions) {
      const deleteTransactionResponse = await page.request.delete(
        `/api/customers/${customer.id}/transactions/${transaction.id}`,
        { headers },
      )
      expect(
        deleteTransactionResponse.ok(),
        await deleteTransactionResponse.text(),
      ).toBeTruthy()
    }

    const deleteCustomerResponse = await page.request.delete(
      `/api/customers/${customer.id}`,
      { headers },
    )
    expect(deleteCustomerResponse.ok(), await deleteCustomerResponse.text()).toBeTruthy()
  }
}

test('customer registration and transaction management are available from the UI', async ({ page }, testInfo) => {
  const suffix = `${Date.now()}-${testInfo.workerIndex}`
  const customerName = `${CUSTOMER_PREFIX}${suffix}`

  await page.goto('/customer/information')
  await cleanupCustomers(page)
  await page.reload()

  await test.step('register a customer from customer management', async () => {
    await page.getByRole('button', { name: '新規登録', exact: true }).click()
    const dialog = page.getByRole('dialog')
    await expect(dialog.getByText('顧客 新規登録', { exact: true })).toBeVisible()
    await dialog.getByLabel('顧客名', { exact: true }).fill(customerName)
    await dialog.getByLabel('ふりがな', { exact: true }).fill('いーつーいーがめんこきゃく')

    const createResponsePromise = page.waitForResponse(response =>
      response.url().endsWith('/api/customers')
      && response.request().method() === 'POST',
    )
    await dialog.getByRole('button', { name: '顧客情報を保存', exact: true }).click()
    const createResponse = await createResponsePromise
    expect(createResponse.status(), await createResponse.text()).toBe(200)

    await expect(dialog).not.toBeVisible()
    await expect(page.getByText(customerName, { exact: true })).toBeVisible()
  })

  const headers = await authenticatedHeaders(page)
  const customersResponse = await page.request.get('/api/customers', { headers })
  expect(customersResponse.ok(), await customersResponse.text()).toBeTruthy()
  const customers = await customersResponse.json() as CustomerListItem[]
  const customer = customers.find(item => item.name === customerName)
  expect(customer, 'registered E2E customer').toBeDefined()
  if (!customer) throw new Error('登録したE2E顧客が見つかりません。')

  const transactionResponse = await page.request.post(
    `/api/customers/${customer.id}/transactions`,
    {
      headers,
      data: {
        targetMonth: '2026-08',
        closingDayRule: { type: 'END_OF_MONTH', value: null, monthOffset: 0 },
        paymentDayRule: { type: 'DAY_OF_MONTH', value: 25, monthOffset: 1 },
        billingAmount: 120_000,
        expectedPaymentDate: '2026-09-25',
        confirmedPaymentDate: null,
        paidAmount: 0,
        fee: 0,
        offsetAmount: 0,
        totalAmount: 0,
        paymentStatus: 'UNPAID',
        note: 'E2E取引管理表示確認',
      },
    },
  )
  expect(transactionResponse.ok(), await transactionResponse.text()).toBeTruthy()

  await test.step('show the customer billing in transaction management', async () => {
    await page.goto('/customer/transaction')
    await expect(page.getByText('取引管理', { exact: true })).toBeVisible()
    await expect(page.getByText('2026-08', { exact: true })).toBeVisible()
    await expect(page.getByText('120,000円', { exact: true })).toHaveCount(2)
    await expect(page.getByText('未入金', { exact: true })).toBeVisible()

    await page.getByText('2026-08', { exact: true }).click()
    const paymentDialog = page.getByRole('dialog')
    await expect(paymentDialog.getByText('入金確認', { exact: true })).toBeVisible()
    await expect(paymentDialog.getByLabel('手数料', { exact: true })).toBeVisible()
    await expect(paymentDialog.getByLabel('入金額', { exact: true })).toBeVisible()
    await expect(paymentDialog.getByLabel('相殺額', { exact: true })).toBeVisible()
    await expect(paymentDialog.getByLabel('入金確認日', { exact: true })).toBeVisible()
    await expect(paymentDialog.getByLabel('備考', { exact: true })).toBeVisible()
    await expect(paymentDialog.getByText('判定予定：未入金', { exact: true })).toBeVisible()
    await paymentDialog.getByRole('button', { name: 'キャンセル', exact: true }).click()
    await expect(paymentDialog).not.toBeVisible()
  })

  await cleanupCustomers(page)
})
