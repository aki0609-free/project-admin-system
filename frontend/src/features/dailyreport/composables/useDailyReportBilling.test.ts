import {
  computed,
  nextTick,
  reactive,
  ref,
  unref,
  type MaybeRef,
} from 'vue'
import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import type { CustomerSiteBillingRateResponse } from '@/features/customer/types/customerApiTypes'
import { createEmptyDailyReportForm } from '@/features/dailyreport/utils/dailyReportFormFactory'

const billingQueryMock = vi.hoisted(() => ({
  customerId: undefined as
    | MaybeRef<number | null | undefined>
    | undefined,
  rates: [] as CustomerSiteBillingRateResponse[],
}))

vi.mock(
  '@/features/customer/api/useCustomerSiteBillingRatesQuery',
  () => ({
    useCustomerSiteBillingRatesQuery: (
      customerId:
        MaybeRef<number | null | undefined>,
    ) => {
      billingQueryMock.customerId = customerId

      return {
        billingRates: computed(
          () => billingQueryMock.rates,
        ),
        isFetching: ref(false),
      }
    },
  }),
)

import { useDailyReportBilling } from './useDailyReportBilling'

const rate = (
  customerSiteId: number,
  jobCode: string,
  jobName: string,
): CustomerSiteBillingRateResponse => ({
  id: customerSiteId,
  customerSiteId,
  jobCode,
  jobName,
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
  note: null,
})

describe('useDailyReportBilling', () => {
  beforeEach(() => {
    billingQueryMock.customerId = undefined
    billingQueryMock.rates = []
  })

  it('顧客IDで取得し、選択中の現場だけを名称候補へ変換する', () => {
    billingQueryMock.rates = [
      rate(101, 'GENERAL_WORK', '一般作業員'),
      rate(202, 'DRIVER', '運転手'),
    ]

    const formModel = reactive(
      createEmptyDailyReportForm(),
    )
    formModel.customerId = 10
    formModel.customerSiteId = 101
    formModel.workDate = '2026-09-05'

    const result = useDailyReportBilling({
      visible: ref(true),
      applyingDetail: ref(false),
      formModel,
    })

    expect(
      unref(billingQueryMock.customerId),
    ).toBe(10)

    expect(result.applicableBillingRates.value)
      .toHaveLength(1)
    expect(result.jobOptions.value).toEqual([
      {
        title: '一般作業員',
        value: 'GENERAL_WORK',
      },
    ])
    expect(result.siteRoleOptions.value).toEqual([
      {
        title: '一般',
        value: 'GENERAL',
      },
    ])
  })

  it('現場に有効な職種が1件だけなら自動選択する', async () => {
    const formModel = reactive(
      createEmptyDailyReportForm(),
    )
    formModel.customerId = 10
    formModel.customerSiteId = 101
    formModel.workDate = '2026-09-05'

    billingQueryMock.rates = [
      rate(101, 'GENERAL_WORK', '一般作業員'),
    ]

    useDailyReportBilling({
      visible: ref(true),
      applyingDetail: ref(false),
      formModel,
    })

    await nextTick()

    expect(formModel.jobCode).toBe('GENERAL_WORK')
    expect(formModel.jobName).toBe('一般作業員')
    expect(formModel.billingRateId).toBe(101)
  })

  it('職種候補が複数ある場合は利用者の選択を待つ', async () => {
    const formModel = reactive(
      createEmptyDailyReportForm(),
    )
    formModel.customerId = 10
    formModel.customerSiteId = 101
    formModel.workDate = '2026-09-05'

    billingQueryMock.rates = [
      rate(101, 'GENERAL_WORK', '一般作業員'),
      rate(101, 'DRIVER', '運転手'),
    ]

    useDailyReportBilling({
      visible: ref(true),
      applyingDetail: ref(false),
      formModel,
    })

    await nextTick()

    expect(formModel.jobCode).toBe('')
    expect(formModel.billingRateId).toBeNull()
  })
})
