import { expect, test, type Page } from '@playwright/test'
import { E2E_EMPLOYEE_CODE } from './support/business-fixture'

type EmployeeListItem = {
  id: number
  employeeCode: string
}

const authHeaders = async (page: Page) => {
  const accessToken = await page.evaluate(() => localStorage.getItem('accessToken'))
  expect(accessToken).not.toBeNull()
  return {
    Authorization: `Bearer ${accessToken}`,
    'X-Tenant-ID': 'default',
  }
}

test('employee loan and saving dialogs use the shared form layout', async ({ page }) => {
  await page.goto('/employee/loan-savings')

  await expect(page.getByRole('heading', { name: '従業員貸付・貯蓄', exact: true })).toBeVisible()

  await page.getByRole('button', { name: '貸付 新規作成', exact: true }).click()
  const loanDialog = page.getByRole('dialog')
  await expect(
    loanDialog.getByRole('heading', { name: '従業員貸付新規作成', exact: true }),
  ).toBeVisible()
  await expect(loanDialog.getByLabel('従業員', { exact: true })).toBeVisible()
  await expect(loanDialog.getByLabel('借入元本', { exact: true })).toBeVisible()
  await expect(loanDialog.getByLabel('借入残高', { exact: true })).toHaveAttribute('readonly', '')
  await expect(loanDialog.getByLabel('承認コメント', { exact: true })).toHaveCount(0)
  await loanDialog.getByRole('button', { name: '閉じる', exact: true }).click()

  await page.getByRole('button', { name: '貯蓄', exact: true }).click()
  await page.getByRole('button', { name: '貯蓄 新規作成', exact: true }).click()
  const savingDialog = page.getByRole('dialog')
  await expect(
    savingDialog.getByRole('heading', { name: '従業員貯蓄新規作成', exact: true }),
  ).toBeVisible()
  await expect(savingDialog.getByLabel('従業員', { exact: true })).toBeVisible()
  await expect(savingDialog.getByLabel('貯蓄率%', { exact: true })).toBeVisible()
  await expect(savingDialog.getByLabel('積立残高', { exact: true })).toHaveAttribute('readonly', '')
  await expect(savingDialog.getByLabel('ID', { exact: true })).toHaveCount(0)
  await expect(savingDialog.getByLabel('承認コメント', { exact: true })).toHaveCount(0)
  await savingDialog.getByRole('button', { name: '閉じる', exact: true }).click()
})

test('loan and saving APIs initialize server-owned balances and allow untouched cleanup', async ({
  page,
}) => {
  await page.goto('/')
  const headers = await authHeaders(page)
  const employeesResponse = await page.request.get('/api/employees', { headers })
  expect(employeesResponse.ok(), await employeesResponse.text()).toBeTruthy()
  const employees = (await employeesResponse.json()) as EmployeeListItem[]
  const employee = employees.find((item) => item.employeeCode === E2E_EMPLOYEE_CODE)
  expect(employee, `fixture employee ${E2E_EMPLOYEE_CODE}`).toBeDefined()
  if (!employee) throw new Error(`fixture employee ${E2E_EMPLOYEE_CODE} was not found`)

  let loanId: number | null = null
  let savingId: number | null = null
  try {
    const loanResponse = await page.request.post('/api/employee/loans', {
      headers,
      data: {
        employeeId: employee.id,
        principal: 123456,
        monthlyRepayment: 10000,
        loanDate: '2026-08-22',
        repaymentStartDate: '2026-09-01',
        activeFlag: false,
      },
    })
    expect(loanResponse.ok(), await loanResponse.text()).toBeTruthy()
    const loan = (await loanResponse.json()) as {
      id: number
      currentBalance: number
      approvalStatus: string
    }
    loanId = loan.id
    expect(loan.currentBalance).toBe(123456)
    expect(loan.approvalStatus).toBe('APPROVED')

    const savingResponse = await page.request.post('/api/employee/savings', {
      headers,
      data: {
        employeeId: employee.id,
        percentage: 5,
        minSalaryThreshold: 200000,
        activeFlag: false,
      },
    })
    expect(savingResponse.ok(), await savingResponse.text()).toBeTruthy()
    const saving = (await savingResponse.json()) as {
      id: number
      currentBalance: number
      approvalStatus: string
    }
    savingId = saving.id
    expect(saving.currentBalance).toBe(0)
    expect(saving.approvalStatus).toBe('APPROVED')
  } finally {
    if (loanId != null) {
      const response = await page.request.delete(`/api/employee/loans/${loanId}`, { headers })
      expect(response.ok(), await response.text()).toBeTruthy()
    }
    if (savingId != null) {
      const response = await page.request.delete(`/api/employee/savings/${savingId}`, { headers })
      expect(response.ok(), await response.text()).toBeTruthy()
    }
  }
})
