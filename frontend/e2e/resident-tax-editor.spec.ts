import { expect, test, type Page } from '@playwright/test'

type ResidentTaxMonth = {
  month: number
  currentTaxAmount: number | null
  draftTaxAmount: number | null
}

type ResidentTaxEmployee = {
  employeeCode: string
  months: ResidentTaxMonth[]
}

type ResidentTaxEditor = {
  status: string | null
  employees: ResidentTaxEmployee[]
}

const authenticatedHeaders = async (page: Page) => {
  const accessToken = await page.evaluate(() => localStorage.getItem('accessToken'))
  expect(accessToken, 'authenticated access token').not.toBeNull()

  return {
    Authorization: `Bearer ${accessToken}`,
    'X-Tenant-ID': 'default',
  }
}

test('resident tax entered from the screen is confirmed for monthly deduction', async ({ page }) => {
  const fiscalYear = 2027
  const targetEmployeeCode = 'E2E-EMP-001'
  const augustTaxAmount = 12_345

  await page.goto('/master/deduction')
  await expect(page.getByText('控除マスター', { exact: true })).toBeVisible()

  await test.step('open the resident tax editor from the deduction master', async () => {
    const residentTaxRow = page.getByRole('row').filter({
      has: page.getByText('住民税', { exact: true }),
    })
    await expect(residentTaxRow).toHaveCount(1)
    await residentTaxRow.click()

    const deductionDialog = page.getByRole('dialog')
    await expect(deductionDialog.getByText('控除 編集', { exact: true })).toBeVisible()
    await deductionDialog.getByRole('button', { name: '詳細情報', exact: true }).click()
    await deductionDialog.getByRole('button', { name: '年度別住民税を編集' }).click()
    await expect(page.getByText('年度別住民税Editor', { exact: true })).toBeVisible()
  })

  const editorDialog = page.getByRole('dialog').filter({
    has: page.getByText('年度別住民税Editor', { exact: true }),
  })

  await test.step('enter and validate the annual resident tax amounts', async () => {
    const yearResponsePromise = page.waitForResponse(response =>
      response.url().includes('/api/master/deductions/resident-tax')
      && response.url().includes(`fiscalYear=${fiscalYear}`)
      && response.request().method() === 'GET',
    )
    const fiscalYearInput = editorDialog.getByLabel('年度（6月～翌年5月）')
    await fiscalYearInput.fill(String(fiscalYear))
    await fiscalYearInput.press('Enter')
    const yearResponse = await yearResponsePromise
    expect(yearResponse.status(), await yearResponse.text()).toBe(200)

    const employeeRow = editorDialog.getByRole('row').filter({
      hasText: targetEmployeeCode,
    })
    await expect(employeeRow).toHaveCount(1)
    const monthInputs = employeeRow.getByRole('spinbutton')
    await expect(monthInputs).toHaveCount(12)

    // 住民税年度の列順は6月、7月、8月…翌年5月。
    await monthInputs.nth(0).fill('13000')
    await monthInputs.nth(1).fill(String(augustTaxAmount))
    await employeeRow.getByRole('button', { name: '7月以降コピー' }).click()

    const draftResponsePromise = page.waitForResponse(response =>
      response.url().endsWith('/api/master/deductions/resident-tax/draft')
      && response.request().method() === 'PUT',
    )
    await editorDialog.getByRole('button', { name: '下書き保存・検証' }).click()
    const draftResponse = await draftResponsePromise
    expect(draftResponse.status(), await draftResponse.text()).toBe(200)
    await expect(editorDialog.getByText(
      '下書きを保存し、入力内容の検証が完了しました。',
      { exact: true },
    )).toBeVisible()
  })

  await test.step('confirm and reload the resident tax values', async () => {
    await editorDialog.getByLabel('変更理由').fill('E2E住民税画面入力確認')
    page.once('dialog', dialog => void dialog.accept())
    const confirmResponsePromise = page.waitForResponse(response =>
      /\/api\/master\/deductions\/resident-tax\/\d+\/confirm$/.test(
        new URL(response.url()).pathname,
      ) && response.request().method() === 'POST',
    )
    await editorDialog.getByRole('button', { name: '確定', exact: true }).click()
    const confirmResponse = await confirmResponsePromise
    expect(confirmResponse.status(), await confirmResponse.text()).toBe(200)

    const headers = await authenticatedHeaders(page)
    const currentResponse = await page.request.get('/api/master/deductions/resident-tax', {
      headers,
      params: { fiscalYear },
    })
    expect(currentResponse.ok(), await currentResponse.text()).toBeTruthy()
    const current = await currentResponse.json() as ResidentTaxEditor
    const employee = current.employees.find(item => item.employeeCode === targetEmployeeCode)
    expect(employee, 'resident tax employee').toBeDefined()
    expect(employee?.months.find(month => month.month === 6)?.currentTaxAmount).toBe(13_000)
    expect(employee?.months.find(month => month.month === 8)?.currentTaxAmount)
      .toBe(augustTaxAmount)
  })
})
