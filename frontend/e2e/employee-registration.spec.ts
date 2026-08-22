import { expect, test, type Page } from '@playwright/test'

type EmployeeListItem = {
  id: number
  employeeCode: string
}

type DailyReportListItem = {
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

const currentBusinessDate = () =>
  new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Tokyo',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date())

const removeTestEmployees = async (page: Page, employeeCode?: string) => {
  const headers = await authenticatedHeaders(page)
  const response = await page.request.get('/api/employees', { headers })
  expect(response.ok(), await response.text()).toBeTruthy()

  const employees = (await response.json()) as EmployeeListItem[]
  const targets = employees.filter((item) =>
    employeeCode ? item.employeeCode === employeeCode : item.employeeCode.startsWith('E2E-UI-'),
  )

  for (const employee of targets) {
    const reportsResponse = await page.request.get('/api/daily-reports', {
      headers,
      params: { employeeId: employee.id },
    })
    expect(reportsResponse.ok(), await reportsResponse.text()).toBeTruthy()
    const reports = (await reportsResponse.json()) as DailyReportListItem[]
    for (const report of reports) {
      const deleteReportResponse = await page.request.delete(`/api/daily-reports/${report.id}`, {
        headers,
      })
      expect(deleteReportResponse.ok(), await deleteReportResponse.text()).toBeTruthy()
    }

    const deleteResponse = await page.request.delete(`/api/employees/${employee.id}`, {
      headers,
    })
    expect(deleteResponse.ok(), await deleteResponse.text()).toBeTruthy()
  }
}

test('employee can be registered with tracked deductions from the employee screen', async ({
  page,
}, testInfo) => {
  test.setTimeout(60_000)
  const uniqueSuffix = `${Date.now()}-${testInfo.workerIndex}`
  const employeeCode = `E2E-UI-${uniqueSuffix}`
  const employeeName = `E2E 画面登録社員 ${uniqueSuffix}`
  // バックエンドの業務日付（Asia/Tokyo）と揃える。
  // UTCのtoISOString()では日本時間の深夜帯に前日となり、当日開始の
  // 従業員別控除設定が日報プレビューから除外されるため使用しない。
  const currentDate = currentBusinessDate()
  const [currentYear, currentMonth, currentDay] = currentDate.split('-').map(Number)
  const currentDateButtonName = new RegExp(`${currentYear}年${currentMonth}月${currentDay}日`)
  let dailyReportId: number | null = null

  await page.goto('/employee/information')
  await removeTestEmployees(page)
  await page.reload()

  await test.step('register an employee and enable tracked deductions', async () => {
    await page.getByRole('button', { name: '新規作成', exact: true }).click()
    await expect(page.getByText('従業員情報新規作成', { exact: true })).toBeVisible()
    const createDialog = page.getByRole('dialog')

    await createDialog.getByLabel('社員コード', { exact: true }).fill(employeeCode)
    await createDialog.getByLabel('氏名', { exact: true }).fill(employeeName)
    await createDialog
      .getByLabel('フリガナ', { exact: true })
      .fill('イーツーイー ガメントウロクシャイン')
    await createDialog.getByLabel('メール', { exact: true }).fill('e2e-ui-employee@example.invalid')

    await createDialog.getByRole('button', { name: '手当・控除設定', exact: true }).click()

    await createDialog.getByRole('tab', { name: '寮費', exact: true }).click()
    await createDialog.getByLabel('この項目を適用する').check()
    const createDormitoryType = createDialog.locator('.v-select').filter({ hasText: '寮タイプ' })
    await createDormitoryType.click()
    await page.getByRole('option', { name: '複数人部屋', exact: true }).click()

    await createDialog.getByRole('tab', { name: '携帯電話貸出料', exact: true }).click()
    await createDialog.getByLabel('この項目を適用する').check()

    await createDialog.getByRole('tab', { name: 'Wi-Fi使用料', exact: true }).click()
    await createDialog.getByLabel('この項目を適用する').check()

    const createResponsePromise = page.waitForResponse(
      (response) =>
        response.url().endsWith('/api/employees') && response.request().method() === 'POST',
    )
    await createDialog.getByRole('button', { name: '保存', exact: true }).click()
    const createResponse = await createResponsePromise
    expect(createResponse.status(), await createResponse.text()).toBe(200)

    await expect(page.getByText('従業員情報新規作成', { exact: true })).not.toBeVisible()
    await expect(page.getByText(employeeName, { exact: true })).toBeVisible()
  })

  await test.step('reopen the employee and verify persisted settings', async () => {
    await page.getByText(employeeName, { exact: true }).click()
    await expect(page.getByText('従業員情報編集', { exact: true })).toBeVisible()
    const editDialog = page.getByRole('dialog')
    await editDialog.getByRole('button', { name: '手当・控除設定', exact: true }).click()

    await editDialog.getByRole('tab', { name: '寮費', exact: true }).click()
    await expect(editDialog.getByLabel('この項目を適用する')).toBeChecked()
    await expect(editDialog.locator('.v-select').filter({ hasText: '寮タイプ' })).toContainText(
      '複数人部屋',
    )

    await editDialog.getByRole('tab', { name: '携帯電話貸出料', exact: true }).click()
    await expect(editDialog.getByLabel('この項目を適用する')).toBeChecked()

    await editDialog.getByRole('tab', { name: 'Wi-Fi使用料', exact: true }).click()
    await expect(editDialog.getByLabel('この項目を適用する')).toBeChecked()
    await expect(editDialog.getByRole('heading', { name: '明細・月次控除' })).toBeVisible()

    await editDialog.getByLabel('控除金額', { exact: true }).fill('1980')
    await editDialog.getByLabel('明細番号（任意）', { exact: true }).fill(`WIFI-${uniqueSuffix}`)
    const transactionResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/payroll-item-transactions') &&
        response.request().method() === 'POST',
    )
    await editDialog.getByRole('button', { name: '明細を追加', exact: true }).click()
    const transactionResponse = await transactionResponsePromise
    expect(transactionResponse.status(), await transactionResponse.text()).toBe(200)
    await expect(editDialog.getByText('1,980円', { exact: true })).toBeVisible()
    await expect(editDialog.getByText(`WIFI-${uniqueSuffix}`, { exact: true })).toBeVisible()

    await editDialog.getByRole('button', { name: '閉じる', exact: true }).click()
  })

  await test.step('override a daily Rule baseline and persist the reason', async () => {
    await page.goto('/operation/daily-reports')
    await page.getByRole('button', { name: '新規作成', exact: true }).click()

    const dialog = page.getByRole('dialog')
    const employeeSelect = dialog.locator('.v-select').filter({ hasText: '従業員' })
    await employeeSelect.click()
    await page
      .getByRole('option', {
        name: `${employeeCode} / ${employeeName}`,
        exact: true,
      })
      .click()

    await dialog.getByLabel('勤務日', { exact: true }).click()
    const datePicker = page.locator('.v-date-picker')
    const initialPreviewPromise = page.waitForResponse(
      (response) =>
        response.url().endsWith('/api/daily-reports/input-items/preview') &&
        response.request().method() === 'POST',
    )
    await datePicker.getByRole('button', { name: currentDateButtonName }).click()
    expect((await initialPreviewPromise).status()).toBe(200)

    await dialog.getByRole('button', { name: '控除', exact: true }).click()
    let dormitoryCard = dialog.locator('.amount-card').filter({ hasText: '寮費' })
    await expect(dormitoryCard).toBeVisible()

    const quantityPreviewPromise = page.waitForResponse((response) => {
      if (
        !response.url().endsWith('/api/daily-reports/input-items/preview') ||
        response.request().method() !== 'POST'
      )
        return false
      const body = response.request().postDataJSON() as {
        deductions?: { deductionCode?: string; quantity?: number }[]
      }
      return (
        body.deductions?.some(
          (item) => item.deductionCode === 'DORMITORY_FEE' && item.quantity === 5,
        ) ?? false
      )
    })
    const dormitoryPaymentDays = dormitoryCard.getByLabel('支払い日数', { exact: true })
    await dormitoryPaymentDays.click()
    await dormitoryPaymentDays.press('ControlOrMeta+A')
    await dormitoryPaymentDays.press('5')
    expect((await quantityPreviewPromise).status()).toBe(200)

    dormitoryCard = dialog.locator('.amount-card').filter({ hasText: '寮費' })
    await expect(dormitoryCard.getByText('Rule基準額：2,250円', { exact: true })).toBeVisible()
    await dormitoryCard.getByLabel('金額', { exact: true }).fill('900')
    await dormitoryCard.getByLabel('金額変更理由', { exact: true }).fill('寮費の日次調整')

    const createReportResponsePromise = page.waitForResponse(
      (response) =>
        response.url().endsWith('/api/daily-reports') && response.request().method() === 'POST',
    )
    await dialog.getByRole('button', { name: '保存', exact: true }).click()
    const createReportResponse = await createReportResponsePromise
    const createReportBody = await createReportResponse.text()
    expect(createReportResponse.status(), createReportBody).toBe(200)
    dailyReportId = (JSON.parse(createReportBody) as { id: number }).id

    await page.getByText(employeeName, { exact: true }).click()
    const editReportDialog = page.getByRole('dialog')
    await editReportDialog.getByRole('button', { name: '控除', exact: true }).click()
    const persistedDormitoryCard = editReportDialog
      .locator('.amount-card')
      .filter({ hasText: '寮費' })
    await expect(persistedDormitoryCard.getByLabel('金額', { exact: true })).toHaveValue('900')
    await expect(persistedDormitoryCard.getByLabel('金額変更理由', { exact: true })).toHaveValue(
      '寮費の日次調整',
    )
    await expect(
      persistedDormitoryCard.getByText('Rule基準額：2,250円', { exact: true }),
    ).toBeVisible()
    await editReportDialog.getByRole('button', { name: '閉じる', exact: true }).click()

    const headers = await authenticatedHeaders(page)
    const deleteReportResponse = await page.request.delete(`/api/daily-reports/${dailyReportId}`, {
      headers,
    })
    expect(deleteReportResponse.ok(), await deleteReportResponse.text()).toBeTruthy()
  })

  await test.step('disable an existing daily deduction setting', async () => {
    await page.goto('/employee/information')
    await page.getByText(employeeName, { exact: true }).click()
    const editDialog = page.getByRole('dialog')
    await editDialog.getByRole('button', { name: '手当・控除設定', exact: true }).click()
    const dormitoryTab = editDialog.getByRole('tab', {
      name: '寮費',
      exact: true,
    })
    await expect(dormitoryTab).toBeVisible()
    await dormitoryTab.click({ force: true })

    await editDialog.getByLabel('この項目を適用する').uncheck()
    const updateResponsePromise = page.waitForResponse(
      (response) =>
        /\/api\/employees\/\d+$/.test(new URL(response.url()).pathname) &&
        response.request().method() === 'PUT',
    )
    await editDialog.getByRole('button', { name: '保存', exact: true }).click()
    const updateResponse = await updateResponsePromise
    expect(updateResponse.status(), await updateResponse.text()).toBe(200)
  })

  await test.step('reflect the changed settings in a new daily report', async () => {
    await page.goto('/operation/daily-reports')
    await page.getByRole('button', { name: '新規作成', exact: true }).click()

    const dailyReportDialog = page.getByRole('dialog')
    await expect(dailyReportDialog.getByText('日報新規作成', { exact: true })).toBeVisible()

    const employeeSelect = dailyReportDialog.locator('.v-select').filter({ hasText: '従業員' })
    await employeeSelect.click()
    await page
      .getByRole('option', {
        name: `${employeeCode} / ${employeeName}`,
        exact: true,
      })
      .click()

    const previewResponsePromise = page.waitForResponse(
      (response) =>
        response.url().endsWith('/api/daily-reports/input-items/preview') &&
        response.request().method() === 'POST',
    )
    await dailyReportDialog.getByLabel('勤務日', { exact: true }).click()
    const currentDatePicker = page.locator('.v-date-picker')
    await expect(currentDatePicker).toBeVisible()
    await currentDatePicker
      .getByRole('button', {
        name: currentDateButtonName,
      })
      .click()
    const previewResponse = await previewResponsePromise
    expect(previewResponse.status(), await previewResponse.text()).toBe(200)

    await dailyReportDialog.getByRole('button', { name: '控除', exact: true }).click()
    await expect(dailyReportDialog.getByText('寮費', { exact: true })).toHaveCount(0)
    await expect(dailyReportDialog.getByText('携帯電話貸出料', { exact: true })).toHaveCount(0)
  })

  await removeTestEmployees(page, employeeCode)
})
