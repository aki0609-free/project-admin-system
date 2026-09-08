import { ref } from 'vue'
import { describe, expect, it } from 'vitest'

import { useDailyReportFormFields } from './useDailyReportFormFields'

describe('useDailyReportFormFields', () => {
  it('請求情報は名称を選択し、内部識別子や重複名称を表示しない', () => {
    const { billingFields, financeFields } =
      useDailyReportFormFields({
        employees: ref([]),
        workDate: ref('2026-09-05'),
        customerOptions: ref([]),
        siteOptions: ref([]),
        jobOptions: ref([
          {
            title: '一般作業員',
            value: 'GENERAL_WORK',
          },
        ]),
        siteRoleOptions: ref([
          {
            title: '一般',
            value: 'GENERAL',
          },
        ]),
        hasActiveLoan: ref(false),
        hasActiveSaving: ref(false),
      })

    expect(
      billingFields.value.map(field => ({
        key: field.key,
        label: field.label,
      })),
    ).toEqual([
      { key: 'jobCode', label: '職種名' },
      {
        key: 'siteRoleCode',
        label: '現場役職名',
      },
      { key: 'billingUnit', label: '単価区分' },
      {
        key: 'billingBaseUnitPrice',
        label: '基準単価',
      },
      {
        key: 'billingOvertimeUnitPrice',
        label: '残業単価',
      },
      {
        key: 'billingNightUnitPrice',
        label: '深夜単価',
      },
      {
        key: 'billingHolidayUnitPrice',
        label: '休日単価',
      },
      {
        key: 'billingCommuteUnitPrice',
        label: '通勤単価',
      },
    ])

    const billingUnitField =
      billingFields.value.find(
        field => field.key === 'billingUnit',
      )

    expect(
      billingUnitField?.formatter?.(
        'DAILY',
        {} as never,
      ),
    ).toBe('日額')

    expect(
      billingFields.value.some(
        field =>
          field.key === 'billingRateId'
          || field.key === 'jobName'
          || field.key === 'siteRoleName',
      ),
    ).toBe(false)

    expect(
      financeFields.value.find(
        field => field.key === 'savingAmount',
      )?.editable,
    ).toBe(false)
    expect(
      financeFields.value.find(
        field =>
          field.key === 'loanRepaymentAmount',
      )?.editable,
    ).toBe(false)
  })
})
