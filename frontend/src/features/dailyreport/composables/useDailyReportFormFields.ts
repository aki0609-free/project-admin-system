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
import { formatCurrency } from '@/shared/utils/CurrencyUtils'
import {
  formatDecimalHoursAsHourMinute,
  formatMinutesAsHourMinute,
  formatNumberWithUnit,
  parseHourMinuteToMinutes,
} from '@/shared/utils/UnitFormatUtils'

type SelectOption<T> = {
  title: string
  value: T
}

type UseDailyReportFormFieldsOptions = {
  employees:
    Ref<EmployeeListItemResponse[]>

  workDate: Ref<string>

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

  hasActiveLoan: Ref<boolean>
    | ComputedRef<boolean>

  hasActiveSaving: Ref<boolean>
    | ComputedRef<boolean>
}

export const useDailyReportFormFields = ({
  employees,
  workDate,
  customerOptions,
  siteOptions,
  jobOptions,
  siteRoleOptions,
  hasActiveLoan,
  hasActiveSaving,
}: UseDailyReportFormFieldsOptions) => {
  const formatBillingUnit = (
    value: unknown,
  ): string => {
    const labels: Record<string, string> = {
      HOURLY: '時間単価',
      DAILY: '日額',
      MONTHLY: '月額',
      FIXED: '固定額',
    }

    return labels[String(value ?? '')]
      ?? String(value ?? '')
  }

  const isEligibleOn = (
    employee: EmployeeListItemResponse,
    targetDate: string,
  ): boolean => {
    if (!targetDate) return employee.activeFlag
    if (employee.hireDate && targetDate < employee.hireDate) return false
    if (employee.resignDate && targetDate > employee.resignDate) return false
    if (employee.contractStartDate && targetDate < employee.contractStartDate) return false
    return !employee.contractEndDate || targetDate <= employee.contractEndDate
  }

  const employeeOptions = computed<
    SelectOption<number>[]
  >(() =>
    employees.value
      .filter(employee => isEligibleOn(employee, workDate.value))
      .map(
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
          editable: false,
          gridColumn: '1 / span 2',
          options:
            customerOptions.value,
        },
        {
          key: 'customerSiteId',
          label: '現場',
          type: 'select',
          editable: false,
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
          label: '休憩時間',
          type: 'text',
          formatter: formatMinutesAsHourMinute,
          parser: parseHourMinuteToMinutes,
        },
        {
          key: 'workHours',
          label: '通常時間',
          type: 'number',
          editable: false,
          formatter: formatDecimalHoursAsHourMinute,
        },
        {
          key: 'overtimeHours',
          label: '早出・残業時間',
          type: 'number',
          editable: false,
          formatter: formatDecimalHoursAsHourMinute,
        },
        {
          key: 'nightWorkHours',
          label: '深夜時間',
          type: 'number',
          editable: false,
          formatter: formatDecimalHoursAsHourMinute,
        },
        {
          key: 'holidayPremiumEligible',
          label: '休日手当対象',
          type: 'checkbox',
          width: 160,
        },
        {
          key: 'holidayWorkHours',
          label: '休日時間',
          type: 'number',
          editable: false,
          formatter: formatDecimalHoursAsHourMinute,
        },
        {
          key: 'vehicleArrangementType',
          label: '車両手配区分',
          type: 'select',
          editable: false,
          options: [
            { title: '車両なし', value: 'NONE' },
            { title: '会社手配（顧客へ距離請求）', value: 'COMPANY' },
            { title: '社員手配・運転者（顧客請求＋運転手当）', value: 'EMPLOYEE' },
            { title: '同乗者（請求・運転手当なし）', value: 'PASSENGER' },
          ],
        },
        {
          key: 'mileage',
          label: '走行距離',
          type: 'number',
          editable: false,
          formatter: value => formatNumberWithUnit(value, 'km'),
        },
        {
          key: 'passengerCount',
          label: '同乗者数',
          type: 'number',
          editable: false,
          formatter: value => formatNumberWithUnit(value, '人'),
        },
        {
          key: 'paidLeaveDays',
          label: '有給取得日数',
          type: 'number',
          formatter: value => formatNumberWithUnit(value, '日'),
        },
        {
          key: 'paidLeaveRemainingDays',
          label: '有給残日数',
          type: 'number',
          editable: false,
          formatter: value => formatNumberWithUnit(value, '日'),
        },
        {
          key:
            'paidLeaveRemainingAfterUsedDays',

          label:
            '有給使用後残',

          type: 'number',
          editable: false,
          formatter: value => formatNumberWithUnit(value, '日'),
        },
        {
          key: 'workDescription',
          label: '備考',
          type: 'textarea',
          rows: 4,
          autoGrow: true,
          gridColumn: '1 / span 4',
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
            label: '職種名',
            type: 'select',
            gridColumn: '1 / span 2',
            options:
              jobOptions.value,
          },
          {
            key: 'siteRoleCode',
            label: '現場役職名',
            type: 'select',
            gridColumn: '3 / span 2',
            options:
              siteRoleOptions.value,
          },
          {
            key: 'billingUnit',
            label: '単価区分',
            type: 'text',
            editable: false,
            formatter: formatBillingUnit,
            gridColumn: '1 / span 2',
          },
          {
            key:
              'billingBaseUnitPrice',

            label:
              '基準単価',

            type: 'number',
            editable: false,
            formatter: value => formatCurrency(Number(value)),
            gridColumn: '3 / span 2',
          },
          {
            key:
              'billingOvertimeUnitPrice',

            label:
              '残業単価',

            type: 'number',
            editable: false,
            formatter: value => formatCurrency(Number(value)),
            gridColumn: '1 / span 2',
          },
          {
            key:
              'billingNightUnitPrice',

            label:
              '深夜単価',

            type: 'number',
            editable: false,
            formatter: value => formatCurrency(Number(value)),
            gridColumn: '3 / span 2',
          },
          {
            key:
              'billingHolidayUnitPrice',

            label:
              '休日単価',

            type: 'number',
            editable: false,
            formatter: value => formatCurrency(Number(value)),
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
            formatter: value => formatCurrency(Number(value)),
            gridColumn: '1 / span 2',
          },
          {
            key:
              'estimatedNetPayAmount',

            label:
              '概算差引支給額',

            type: 'number',
            editable: false,
            formatter: value => formatCurrency(Number(value)),
            gridColumn: '3 / span 2',
          },
          {
            key: 'savingBalance',
            label: '貯蓄残高',
            type: 'number',
            editable: false,
            formatter: value => formatCurrency(Number(value)),
            gridColumn: '1 / span 2',
          },
          {
            key: 'loanBalance',
            label: '借入残高',
            type: 'number',
            editable: false,
            formatter: value => formatCurrency(Number(value)),
            gridColumn: '3 / span 2',
          },
          {
            key: 'savingAmount',
            label: '実際貯蓄額',
            type: 'number',
            editable: hasActiveSaving.value,
            formatter: value => formatCurrency(Number(value)),
            gridColumn: '1 / span 2',
          },
          {
            key:
              'loanRepaymentAmount',

            label:
              '実際返済額',

            type: 'number',
            editable: hasActiveLoan.value,
            formatter: value => formatCurrency(Number(value)),
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
