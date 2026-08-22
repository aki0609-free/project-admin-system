import { expect, type APIResponse, type Page } from '@playwright/test'

export const E2E_EMPLOYEE_CODE = 'E2E-EMP-001'
export const E2E_EMPLOYEE_NAME = 'E2E 給与検証社員'
export const E2E_WORK_DATE = '2026-08-10'
export const E2E_TARGET_MONTH = '2026-08'
export const E2E_CUSTOMER_NAME = 'E2E 月間集計検証顧客'
export const E2E_SITE_NAME = 'E2E 東京検証現場'
export const E2E_JOB_CODE = 'E2E_GENERAL_WORK'
export const E2E_JOB_NAME = 'E2E 一般作業員'

type EmployeeListItem = {
  id: number
  employeeCode: string
}

type DailyReport = {
  id: number
  employeeId: number
  workDate: string
  normalPayAmount: number
  overtimePayAmount: number
  estimatedGrossPayAmount: number
  estimatedNetPayAmount: number
  billingUnit: string | null
  billingBaseUnitPrice: number
}

type CustomerListItem = {
  id: number
  name: string
}

type CustomerSite = {
  id: number
  name: string
}

type CustomerDetail = CustomerListItem & {
  furiganaName: string | null
  shortName: string | null
  postNo: string | null
  address: string | null
  representativeName: string | null
  phone: string | null
  jobType: string | null
  contractFlag: string | null
  invoiceType: 'PATTERN_1' | 'PATTERN_2' | 'PATTERN_3'
  closingDayRule: Record<string, unknown> | null
  paymentDayRule: Record<string, unknown> | null
  sites: CustomerSite[]
}

type CustomerBillingRate = {
  id: number
  customerSiteId: number
  jobCode: string
}

type MonthlySummaryFixture = {
  customerId: number
  customerSiteId: number
}

type ResidentTaxMonth = {
  month: number
  currentTaxAmount: number | null
}

type ResidentTaxEmployee = {
  employeeId: number
  months: ResidentTaxMonth[]
}

type ResidentTaxEditor = {
  batchId: number | null
  employees: ResidentTaxEmployee[]
}

type DeductionMaster = {
  id: number
  deductionCode: string
  deductionName: string
  deductionType: string | null
  calculationType: string | null
  deductionUnit: string | null
  detailViewType: string | null
  ruleName: string | null
  defaultAmount: number | null
  allowManualInput: boolean | null
  minAmount: number | null
  maxAmount: number | null
  showOnDailyStatement: boolean | null
  showOnMonthlyStatement: boolean | null
  carryToMonthlySettlement: boolean | null
  displayOrder: number | null
  enabled: boolean | null
  note: string | null
  policy: Record<string, unknown>
}

type DormitoryFee = {
  dormitoryType: 'SINGLE_ROOM' | 'SHARED_ROOM'
  dailyAmount: number
  activeFlag: boolean
}

const json = async <T>(response: APIResponse): Promise<T> => {
  const body = await response.text()
  expect(response.ok(), `${response.status()} ${response.url()}\n${body}`).toBeTruthy()
  return body ? JSON.parse(body) as T : {} as T
}

const headers = async (page: Page) => {
  const accessToken = await page.evaluate(() => localStorage.getItem('accessToken'))
  expect(accessToken, 'authenticated access token').not.toBeNull()
  return {
    Authorization: `Bearer ${accessToken}`,
    'X-Tenant-ID': 'default',
  }
}

const employeeRequest = {
  employeeCode: E2E_EMPLOYEE_CODE,
  employeeName: E2E_EMPLOYEE_NAME,
  employeeNameKana: 'イーツーイー キュウヨケンショウシャイン',
  gender: null,
  birthDate: '1990-01-15',
  hireDate: '2026-04-01',
  resignDate: null,
  employmentType: 'FULL_TIME',
  employmentStatus: 'ACTIVE',
  phone: null,
  email: 'e2e-employee@example.invalid',
  postalCode: null,
  address: null,
  dormitoryFlag: true,
  dormitoryType: 'SHARED_ROOM',
  activeFlag: true,
  payrollProfile: {
    taxCategory: 'KOU',
    taxDependentCount: 0,
    dependentFlag: false,
    dependentOfOtherFlag: false,
    paidLeaveRemainingDays: 10,
    incomeTaxCalcFlag: true,
    residentTaxCalcFlag: true,
    residentTaxMonthly: 0,
    employmentInsuranceFlag: true,
    socialInsuranceFlag: true,
    healthInsuranceFlag: true,
    pensionInsuranceFlag: true,
    careInsuranceFlag: false,
    dailyPayFlag: false,
    commuteAllowanceMonthly: 0,
  },
  contract: {
    contractStartDate: '2026-04-01',
    contractEndDate: null,
    renewalFlag: false,
    salaryType: 'HOURLY',
    paymentCycle: 'MONTHLY',
    monthlySalary: 0,
    weeklyWage: 0,
    dailyWage: 0,
    hourlyWage: 1500,
    standardWorkingHours: 40,
    note: 'Playwright固定業務データ',
  },
  payrollItemSettings: [
    {
      targetType: 'DEDUCTION',
      targetCode: 'DORMITORY_FEE',
      enabled: true,
      parameters: { dormitoryType: 'SHARED_ROOM' },
    },
    {
      targetType: 'DEDUCTION',
      targetCode: 'MOBILE_RENTAL',
      enabled: true,
      parameters: {},
    },
  ],
}

const dailyReportRequest = (
  employeeId: number,
  dormitoryMasterId: number,
  monthlySummaryFixture: MonthlySummaryFixture,
) => ({
  employeeId,
  workDate: E2E_WORK_DATE,
  paymentDate: E2E_WORK_DATE,
  customerId: monthlySummaryFixture.customerId,
  customerSiteId: monthlySummaryFixture.customerSiteId,
  customerName: E2E_CUSTOMER_NAME,
  siteName: E2E_SITE_NAME,
  jobCode: E2E_JOB_CODE,
  jobName: E2E_JOB_NAME,
  siteRoleCode: 'GENERAL',
  siteRoleName: '一般',
  workDescription: 'Playwright固定日報・月間集計表検証',
  startTime: '08:00',
  endTime: '18:00',
  breakMinutes: 60,
  workHours: 8,
  overtimeHours: 2,
  nightWorkHours: 0,
  holidayWorkHours: 0,
  allowanceAmount: 0,
  deductionAmount: 0,
  loanRepaymentAmount: 0,
  savingAmount: 0,
  dormitoryChargeDays: 3,
  vehicleUsedFlag: false,
  mileage: 0,
  paidLeaveDays: 0,
  approvalStatus: 'APPROVED',
  approvalComment: 'E2E自動承認',
  allowances: [],
  deductions: [
    {
      deductionMasterId: dormitoryMasterId,
      deductionCode: 'DORMITORY_FEE',
      deductionName: '寮費',
      calculatedAmount: 0,
      amount: 0,
      manualOverride: false,
      overrideReason: null,
      quantity: 3,
      balanceUnit: 'DAYS',
    },
  ],
})

const residentTaxMonths = () => [
  { month: 6, taxAmount: 12_000 },
  ...[7, 8, 9, 10, 11, 12, 1, 2, 3, 4, 5]
    .map(month => ({ month, taxAmount: 11_000 })),
]

const customerRequest = (site?: CustomerSite) => ({
  name: E2E_CUSTOMER_NAME,
  furiganaName: 'いーつーいーげつかんしゅうけいけんしょうこきゃく',
  shortName: 'E2E検証顧客',
  postNo: '100-0001',
  address: '東京都千代田区E2E 1-1',
  representativeName: 'E2E 担当者',
  phone: '03-0000-0000',
  jobType: '建設',
  contractFlag: '契約中',
  invoiceType: 'PATTERN_1',
  closingDayRule: { type: 'END_OF_MONTH', value: null, monthOffset: 0 },
  paymentDayRule: { type: 'DAY_OF_MONTH', value: 25, monthOffset: 1 },
  sites: [{
    id: site?.id ?? null,
    name: E2E_SITE_NAME,
    contactPersonName: 'E2E 現場責任者',
    contactPersonPhone: '090-0000-0000',
    contactPersonEmail: 'e2e-site@example.invalid',
    distanceFromCompanyKm: 15,
    _isNew: site == null,
    _isUpdated: site != null,
    _isDeleted: false,
  }],
  employees: [],
})

async function ensureMonthlySummaryFixture(
  page: Page,
  requestHeaders: Record<string, string>,
): Promise<MonthlySummaryFixture> {
  const customers = await json<CustomerListItem[]>(
    await page.request.get('/api/customers', { headers: requestHeaders }),
  )
  let customerId = customers.find(customer => customer.name === E2E_CUSTOMER_NAME)?.id

  if (customerId == null) {
    customerId = await json<number>(
      await page.request.post('/api/customers', {
        headers: requestHeaders,
        data: customerRequest(),
      }),
    )
  }

  let customer = await json<CustomerDetail>(
    await page.request.get(`/api/customers/${customerId}`, { headers: requestHeaders }),
  )
  let site = customer.sites.find(item => item.name === E2E_SITE_NAME)
  if (!site) {
    await json(
      await page.request.put(`/api/customers/${customerId}`, {
        headers: requestHeaders,
        data: customerRequest(),
      }),
    )
    customer = await json<CustomerDetail>(
      await page.request.get(`/api/customers/${customerId}`, { headers: requestHeaders }),
    )
    site = customer.sites.find(item => item.name === E2E_SITE_NAME)
  }
  expect(site, 'monthly summary customer site').toBeDefined()
  if (!site) throw new Error('月間集計表のE2E現場を作成できませんでした。')

  const rates = await json<CustomerBillingRate[]>(
    await page.request.get(`/api/customers/${customerId}/billing-rates`, {
      headers: requestHeaders,
    }),
  )
  const existingRate = rates.find(rate =>
    rate.customerSiteId === site.id && rate.jobCode === E2E_JOB_CODE)
  const rateRequest = {
    id: existingRate?.id ?? null,
    customerSiteId: site.id,
    jobCode: E2E_JOB_CODE,
    jobName: E2E_JOB_NAME,
    siteRoleCode: 'GENERAL',
    siteRoleName: '一般',
    billingUnit: 'DAILY',
    baseUnitPrice: 22_000,
    overtimeUnitPrice: 2_750,
    nightUnitPrice: 3_300,
    holidayUnitPrice: 29_700,
    commuteUnitPrice: 30,
    effectiveFrom: '2026-04-01',
    effectiveTo: null,
    displayOrder: 10,
    activeFlag: true,
    note: 'Playwright月間集計表確認用の日単価',
    _isNew: existingRate == null,
    _isUpdated: existingRate != null,
    _isDeleted: false,
  }
  await json(
    existingRate
      ? await page.request.put(
          `/api/customers/${customerId}/billing-rates/${existingRate.id}`,
          { headers: requestHeaders, data: rateRequest },
        )
      : await page.request.post(`/api/customers/${customerId}/billing-rates`, {
          headers: requestHeaders,
          data: rateRequest,
        }),
  )

  return { customerId, customerSiteId: site.id }
}

export const ensureBusinessFixture = async (page: Page) => {
  const requestHeaders = await headers(page)
  const monthlySummaryFixture = await ensureMonthlySummaryFixture(
    page,
    requestHeaders,
  )
  const deductions = await json<DeductionMaster[]>(
    await page.request.get('/api/master/deductions', { headers: requestHeaders }),
  )
  const dormitoryMaster = deductions.find(
    deduction => deduction.deductionCode === 'DORMITORY_FEE',
  )
  const mobileMaster = deductions.find(
    deduction => deduction.deductionCode === 'MOBILE_RENTAL',
  )
  expect(dormitoryMaster, 'DORMITORY_FEE master').toBeDefined()
  expect(mobileMaster, 'MOBILE_RENTAL master').toBeDefined()
  if (!dormitoryMaster || !mobileMaster) {
    throw new Error('E2E用の寮費・携帯電話貸出料マスターがありません。')
  }

  const dormitoryDetail = await json<DeductionMaster>(
    await page.request.get(`/api/master/deductions/${dormitoryMaster.id}`, {
      headers: requestHeaders,
    }),
  )
  const mobileDetail = await json<DeductionMaster>(
    await page.request.get(`/api/master/deductions/${mobileMaster.id}`, {
      headers: requestHeaders,
    }),
  )

  await json(
    await page.request.put(`/api/master/deductions/${dormitoryMaster.id}`, {
      headers: requestHeaders,
      data: {
        deductionCode: dormitoryMaster.deductionCode,
        deductionName: '寮費',
        deductionType: dormitoryMaster.deductionType ?? 'COMPANY',
        calculationType: 'AUTO',
        deductionUnit: 'BOTH',
        detailViewType: dormitoryMaster.detailViewType ?? 'NONE',
        ruleName: dormitoryMaster.ruleName ?? 'DORMITORY_DAILY_FEE',
        defaultAmount: 0,
        allowManualInput: true,
        minAmount: dormitoryMaster.minAmount,
        maxAmount: dormitoryMaster.maxAmount,
        showOnDailyStatement: true,
        showOnMonthlyStatement: true,
        carryToMonthlySettlement: true,
        displayOrder: dormitoryMaster.displayOrder ?? 110,
        enabled: true,
        note: 'Playwright固定業務データ（日次寮費）',
        policy: {
          ...dormitoryDetail.policy,
          applicationScope: 'EMPLOYEE_ENROLLMENT',
        },
      },
    }),
  )

  await json(
    await page.request.put(`/api/master/deductions/${mobileMaster.id}`, {
      headers: requestHeaders,
      data: {
        deductionCode: mobileMaster.deductionCode,
        deductionName: '携帯電話貸出料',
        deductionType: mobileMaster.deductionType ?? 'COMPANY',
        calculationType: 'FIXED',
        deductionUnit: 'MONTHLY',
        detailViewType: mobileMaster.detailViewType ?? 'NONE',
        ruleName: null,
        defaultAmount: 200,
        allowManualInput: true,
        minAmount: mobileMaster.minAmount,
        maxAmount: mobileMaster.maxAmount,
        showOnDailyStatement: false,
        showOnMonthlyStatement: true,
        carryToMonthlySettlement: true,
        displayOrder: mobileMaster.displayOrder ?? 120,
        enabled: true,
        note: 'Playwright固定業務データ（明細到着時の控除取引）',
        policy: {
          ...mobileDetail.policy,
          applicationScope: 'EMPLOYEE_ENROLLMENT',
        },
      },
    }),
  )

  const dormitoryFees = await json<DormitoryFee[]>(
    await page.request.get('/api/admin/business-settings/dormitory-fees', {
      headers: requestHeaders,
    }),
  )
  await json<DormitoryFee[]>(
    await page.request.put('/api/admin/business-settings/dormitory-fees', {
      headers: requestHeaders,
      data: dormitoryFees.map(fee => ({
        dormitoryType: fee.dormitoryType,
        dailyAmount: fee.dormitoryType === 'SHARED_ROOM' ? 450 : fee.dailyAmount,
        activeFlag: true,
      })),
    }),
  )

  const employees = await json<EmployeeListItem[]>(
    await page.request.get('/api/employees', { headers: requestHeaders }),
  )
  const existingEmployee = employees.find(
    employee => employee.employeeCode === E2E_EMPLOYEE_CODE,
  )
  const employee = await json<{ id: number }>(
    existingEmployee
      ? await page.request.put(`/api/employees/${existingEmployee.id}`, {
          headers: requestHeaders,
          data: employeeRequest,
        })
      : await page.request.post('/api/employees', {
          headers: requestHeaders,
          data: employeeRequest,
        }),
  )

  const taxEditor = await json<ResidentTaxEditor>(
    await page.request.get('/api/master/deductions/resident-tax', {
      headers: requestHeaders,
      params: { fiscalYear: 2026 },
    }),
  )
  const taxEmployee = taxEditor.employees.find(row => row.employeeId === employee.id)
  const residentTaxCurrent = taxEmployee?.months.find(month => month.month === 8)
    ?.currentTaxAmount
  if (residentTaxCurrent !== 11_000) {
    const draft = await json<ResidentTaxEditor>(
      await page.request.put('/api/master/deductions/resident-tax/draft', {
        headers: requestHeaders,
        data: {
          fiscalYear: 2026,
          employees: [{ employeeId: employee.id, months: residentTaxMonths() }],
        },
      }),
    )
    expect(draft.batchId, 'resident tax draft batch').not.toBeNull()
    await json<ResidentTaxEditor>(
      await page.request.post(
        `/api/master/deductions/resident-tax/${draft.batchId}/confirm`,
        {
          headers: requestHeaders,
          data: {
            changeReason: 'Playwright固定業務データ',
            acknowledgeReclosing: false,
          },
        },
      ),
    )
  }

  const reports = await json<DailyReport[]>(
    await page.request.get('/api/daily-reports', {
      headers: requestHeaders,
      params: {
        from: E2E_WORK_DATE,
        to: E2E_WORK_DATE,
        employeeId: employee.id,
      },
    }),
  )
  const existingReport = reports.find(report => report.workDate === E2E_WORK_DATE)
  const report = await json<DailyReport>(
    existingReport
      ? await page.request.put(`/api/daily-reports/${existingReport.id}`, {
          headers: requestHeaders,
          data: dailyReportRequest(
            employee.id,
            dormitoryMaster.id,
            monthlySummaryFixture,
          ),
        })
      : await page.request.post('/api/daily-reports', {
          headers: requestHeaders,
          data: dailyReportRequest(
            employee.id,
            dormitoryMaster.id,
            monthlySummaryFixture,
          ),
        }),
  )

  expect(report.normalPayAmount).toBe(12_000)
  expect(report.overtimePayAmount).toBe(3_750)
  expect(report.estimatedGrossPayAmount).toBe(15_750)
  expect(report.estimatedNetPayAmount).toBeLessThanOrEqual(
    report.estimatedGrossPayAmount,
  )
  expect(report.billingUnit).toBe('DAILY')
  expect(report.billingBaseUnitPrice).toBe(22_000)

  return { employeeId: employee.id, reportId: report.id }
}
