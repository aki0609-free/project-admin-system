import {
  computed,
  type ComputedRef,
  type Ref,
} from 'vue'

import type {
  GridFormFieldDef,
} from '@/shared/components/form/grid_based_form/types/types'

import type {
  DailyReportForm,
} from '@/features/dailyreport/types/dailyReportFormTypes'

import type {
  EmployeeListItemResponse,
} from '@/features/employees/types/employeeApiTypes'

import {
  normalizeTimeHHmm,
} from '@/shared/utils/TimeUtils'

type SelectOption<T> = {
  title: string
  value: T
}

type UseDailyReportFormFieldsOptions = {
  employees:
    Ref<EmployeeListItemResponse[]>

  customerOptions:
    Ref<SelectOption<number>[]>
    | ComputedRef<SelectOption<number>[]>

  siteOptions:
    Ref<SelectOption<number>[]>
    | ComputedRef<SelectOption<number>[]>

  jobOptions:
    Ref<SelectOption<string>[]>
    | ComputedRef<SelectOption<string>[]>

  siteRoleOptions:
    Ref<SelectOption<string>[]>
    | ComputedRef<SelectOption<string>[]>
}

export const useDailyReportFormFields = ({
  employees,
  customerOptions,
  siteOptions,
  jobOptions,
  siteRoleOptions,
}: UseDailyReportFormFieldsOptions) => {
  const employeeOptions = computed<
    SelectOption<number>[]
  >(() =>
    employees.value.map(
      employee => ({
        title:
          `${employee.employeeCode} / ${employee.employeeName}`,

        value:
          employee.id,
      }),
    ),
  )

  const fields = computed(() => {
    const result:
      GridFormFieldDef<DailyReportForm>[] = [
        {
          key: 'employeeId',
          label: '従業員',
          type: 'select',
          gridColumn: '1 / span 2',
          options:
            employeeOptions.value,
        },
        {
          key: 'workDate',
          label: '勤務日',
          type: 'date',
          gridColumn: '3 / span 1',
        },
        {
          key: 'paymentDate',
          label: '支払日',
          type: 'date',
          gridColumn: '4 / span 1',
        },
        {
          key: 'customerId',
          label: '顧客',
          type: 'select',
          gridColumn: '1 / span 2',
          options:
            customerOptions.value,
        },
        {
          key: 'customerSiteId',
          label: '現場',
          type: 'select',
          gridColumn: '3 / span 2',
          options:
            siteOptions.value,
        },
        {
          key: 'startTime',
          label: '開始時刻',
          type: 'time',
          gridColumn: '1 / span 2',
          formatter: value =>
            normalizeTimeHHmm(
              String(value ?? ''),
            ),
        },
        {
          key: 'endTime',
          label: '終了時刻',
          type: 'time',
          gridColumn: '3 / span 2',
          formatter: value =>
            normalizeTimeHHmm(
              String(value ?? ''),
            ),
        },
        {
          key: 'breakMinutes',
          label: '休憩分',
          type: 'number',
        },
        {
          key: 'workHours',
          label: '通常時間',
          type: 'number',
        },
        {
          key: 'overtimeHours',
          label: '早出・残業時間',
          type: 'number',
        },
        {
          key: 'nightWorkHours',
          label: '深夜時間',
          type: 'number',
        },
        {
          key: 'holidayWorkHours',
          label: '休日時間',
          type: 'number',
        },
        {
          key: 'vehicleUsedFlag',
          label: '車両使用',
          type: 'checkbox',
          width: 120,
        },
        {
          key: 'mileage',
          label: '走行距離',
          type: 'number',
        },
        {
          key: 'paidLeaveDays',
          label: '有給取得日数',
          type: 'number',
        },
        {
          key: 'paidLeaveRemainingDays',
          label: '有給残日数',
          type: 'number',
          editable: false,
        },
        {
          key:
            'paidLeaveRemainingAfterUsedDays',

          label:
            '有給使用後残',

          type: 'number',
          editable: false,
        },
      ]

    return result
  })

  const billingFields =
    computed(() => {
      const result:
        GridFormFieldDef<DailyReportForm>[] = [
          {
            key: 'jobCode',
            label: '職種',
            type: 'select',
            gridColumn: '1 / span 2',
            options:
              jobOptions.value,
          },
          {
            key: 'siteRoleCode',
            label: '現場役職',
            type: 'select',
            gridColumn: '3 / span 2',
            options:
              siteRoleOptions.value,
          },
          {
            key: 'jobName',
            label: '職種名',
            type: 'text',
            editable: false,
            gridColumn: '1 / span 2',
          },
          {
            key: 'siteRoleName',
            label: '現場役職名',
            type: 'text',
            editable: false,
            gridColumn: '3 / span 2',
          },
          {
            key: 'billingRateId',
            label: '適用単価ID',
            type: 'number',
            editable: false,
            gridColumn: '1 / span 2',
          },
          {
            key: 'billingUnit',
            label: '単価区分',
            type: 'text',
            editable: false,
            gridColumn: '3 / span 2',
          },
          {
            key:
              'billingBaseUnitPrice',

            label:
              '基準単価',

            type: 'number',
            editable: false,
            gridColumn: '1 / span 2',
          },
          {
            key:
              'billingOvertimeUnitPrice',

            label:
              '残業単価',

            type: 'number',
            editable: false,
            gridColumn: '3 / span 2',
          },
          {
            key:
              'billingNightUnitPrice',

            label:
              '深夜単価',

            type: 'number',
            editable: false,
            gridColumn: '1 / span 2',
          },
          {
            key:
              'billingHolidayUnitPrice',

            label:
              '休日単価',

            type: 'number',
            editable: false,
            gridColumn: '3 / span 2',
          },
          {
            key:
              'billingCommuteUnitPrice',

            label:
              '通勤単価',

            type: 'number',
            editable: false,
            gridColumn: '1 / span 2',
          },
        ]

      return result
    })

  const financeFields =
    computed(() => {
      const result:
        GridFormFieldDef<DailyReportForm>[] = [
          {
            key:
              'estimatedGrossPayAmount',

            label:
              '概算支給額',

            type: 'number',
            editable: false,
            gridColumn: '1 / span 2',
          },
          {
            key:
              'estimatedNetPayAmount',

            label:
              '概算差引支給額',

            type: 'number',
            editable: false,
            gridColumn: '3 / span 2',
          },
          {
            key: 'savingBalance',
            label: '貯蓄残高',
            type: 'number',
            editable: false,
            gridColumn: '1 / span 2',
          },
          {
            key: 'loanBalance',
            label: '借入残高',
            type: 'number',
            editable: false,
            gridColumn: '3 / span 2',
          },
          {
            key: 'savingAmount',
            label: '実際貯蓄額',
            type: 'number',
            gridColumn: '1 / span 2',
          },
          {
            key:
              'loanRepaymentAmount',

            label:
              '実際返済額',

            type: 'number',
            gridColumn: '3 / span 2',
          },
        ]

      return result
    })

  return {
    fields,
    billingFields,
    financeFields,
  }
}