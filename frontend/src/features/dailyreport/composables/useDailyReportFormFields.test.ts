import { ref } from 'vue'
import { describe, expect, it, vi } from 'vitest'

import { useDailyReportFormFields } from './useDailyReportFormFields'

describe('useDailyReportFormFields', () => {
  it('請求情報は名称を選択し、内部識別子や重複名称を表示しない', () => {
    const onPaymentDateInput = vi.fn()
    const { fields, vehicleFields, billingFields, financeFields } =
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
        onPaymentDateInput,
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
        field => field.key === 'savingWithdrawalAmount',
      )?.editable,
    ).toBe(false)
    expect(
      financeFields.value.find(
        field =>
          field.key === 'loanRepaymentAmount',
      )?.editable,
    ).toBe(false)

    expect(
      fields.value.find(
        field => field.key === 'workHours',
      )?.formatter?.(1.5, {} as never),
    ).toBe('1:30')
    expect(
      fields.value.find(
        field => field.key === 'breakMinutes',
      )?.formatter?.(90, {} as never),
    ).toBe('1:30')
    expect(
      fields.value.find(
        field => field.key === 'breakMinutes',
      )?.parser?.('1:30', {} as never),
    ).toBe(90)
    expect(
      fields.value.find(
        field => field.key === 'workHours',
      )?.parser?.('1:30', {} as never),
    ).toBe(1.5)
    expect(
      fields.value.find(
        field => field.key === 'paidLeaveDays',
      )?.formatter?.(0.5, {} as never),
    ).toBe('0.5日')

    fields.value.find(
      field => field.key === 'paymentDate',
    )?.onUpdate?.('2026-09-08', {} as never)
    expect(onPaymentDateInput).toHaveBeenCalledOnce()

    for (const key of [
      'holidayWorkHours',
    ] as const) {
      expect(
        fields.value.find(field => field.key === key)?.editable,
      ).toBe(false)
    }

    expect(
      fields.value.some(field => [
        'vehicleArrangementType',
        'mileage',
        'passengerCount',
        'paidLeaveRemainingAfterUsedDays',
      ].includes(String(field.key))),
    ).toBe(false)

    expect(vehicleFields.value.map(field => field.key)).toEqual([
      'vehicleArrangementType',
      'mileage',
      'passengerCount',
    ])
    expect(
      vehicleFields.value.every(field => field.editable === false),
    ).toBe(true)

    expect(
      financeFields.value.map(field => [field.key, field.gridColumn]),
    ).toEqual([
      ['estimatedGrossPayAmount', '1 / span 4'],
      ['estimatedNetPayAmount', '1 / span 2'],
      ['savingBalance', '3 / span 2'],
      ['loanBalance', '1 / span 2'],
      ['savingAmount', '3 / span 2'],
      ['loanRepaymentAmount', '1 / span 2'],
      ['savingWithdrawalAmount', '3 / span 2'],
    ])

    for (const key of [
      'workHours',
      'overtimeHours',
      'nightWorkHours',
    ] as const) {
      expect(
        fields.value.find(field => field.key === key)?.editable,
      ).not.toBe(false)
      expect(
        fields.value.find(field => field.key === key)?.type,
      ).toBe('text')
    }
  })
})
