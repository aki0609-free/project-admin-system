import {
  computed,
  reactive,
  ref,
  watch,
  type Ref,
} from 'vue'

import {
  storeToRefs,
} from 'pinia'

import type {
  ToolbarItem,
} from '@/shared/components/toolbar/types/types'

import type {
  DailyReportDetailResponse,
} from '@/features/dailyreport/types/dailyReportApiTypes'

import type {
  DailyReportForm,
} from '@/features/dailyreport/types/dailyReportFormTypes'

import type {
  DailyReportInputResponse,
} from '@/features/dailyreport/types/dailyReportInputItemTypes'

import {
  createEmptyDailyReportForm,
  toDailyReportForm,
} from '@/features/dailyreport/utils/dailyReportFormFactory'

import type {
  EmployeeListItemResponse,
} from '@/features/employees/types/employeeApiTypes'

import type {
  EmployeeContractQueryResponse,
} from '@/features/employees/types/employeeWorkApiTypes'

import {
  useEmployeeFinanceSummaryQuery,
} from '@/features/employees/api/useEmployeeFinanceSummaryQuery'

import {
  useEmployeeContractQuery,
} from '@/features/employees/api/useEmployeeContractQuery'

import {
  useCustomerMasterStore,
} from '@/features/customer/store/useCustomerMasterStore'

import {
  calculateDailyReportWorkTimes,
} from '@/features/dailyreport/utils/dailyReportTimeCalculator'

import {
  dailyReportSchema,
} from '@/features/dailyreport/schemas/dailyReportSchema'

import {
  useDailyReportBilling,
} from './useDailyReportBilling'

import {
  useDailyReportFormFields,
} from './useDailyReportFormFields'

import type {
  DailyReportCreateParams,
} from './useDailyReportDialog'

type DailyReportTab =
  | 'basic'
  | 'billing'
  | 'allowance'
  | 'deduction'
  | 'finance'
  | 'approval'

export const useDailyReportEditDialog = (
  visible: Ref<boolean>,
  dailyReport: Ref<DailyReportDetailResponse | null>,
  createParams: Ref<DailyReportCreateParams | null>,
  employees: Ref<EmployeeListItemResponse[]>,
  inputItems: Ref<DailyReportInputResponse>,
  emitSave: (form: DailyReportForm) => void,
  emitDelete: (form: DailyReportForm) => void,
) => {
  const customerStore =
    useCustomerMasterStore()

  const {
    customerOptions,
  } = storeToRefs(customerStore)

  const activeTab =
    ref<DailyReportTab>('basic')

  const applyingDetail =
    ref(false)

  const formModel =
    reactive<DailyReportForm>(
      createEmptyDailyReportForm(),
    )

  const financeQuery =
    useEmployeeFinanceSummaryQuery(
      computed(
        () => formModel.employeeId,
      ),
    )

  const contractQuery =
    useEmployeeContractQuery(
      computed(
        () => formModel.employeeId,
      ),
    )

  const siteOptions = computed(() =>
    customerStore.siteOptions(
      formModel.customerId,
    ),
  )

  const {
    billingRateLoading,
    applicableBillingRates,
    selectedApplicableBillingRate,
    jobOptions,
    siteRoleOptions,
    clearBillingSelection,
    applyBillingRatePreview,
  } = useDailyReportBilling({
    visible,
    applyingDetail,
    formModel,
  })

  const {
    fields,
    billingFields,
    financeFields,
  } = useDailyReportFormFields({
    employees,
    customerOptions,
    siteOptions,
    jobOptions,
    siteRoleOptions,
  })

  const nvl = (
    value: number | null | undefined,
  ): number =>
    Number(value ?? 0)

  const calculateBasePayAmount = (
    contract: EmployeeContractQueryResponse | null,
  ): number => {
    if (!contract?.salaryType) {
      return 0
    }

    switch (contract.salaryType) {
      case 'MONTHLY':
        return 0

      case 'WEEKLY': {
        const weeklyWage =
          nvl(contract.weeklyWage)

        const standardWeeklyHours =
          nvl(
            contract.standardWorkingHours,
          ) > 0
            ? nvl(
              contract.standardWorkingHours,
            )
            : 40

        /*
         * 深夜時間は時間帯区分であり、
         * 通常・残業・休日時間と重複するため
         * 実労働時間には加算しない。
         */
        const totalHours =
          nvl(formModel.workHours)
          + nvl(formModel.overtimeHours)
          + nvl(formModel.holidayWorkHours)

        return weeklyWage
          * (
            totalHours
            / standardWeeklyHours
          )
      }

      case 'DAILY':
        return nvl(
          contract.dailyWage,
        )

      case 'HOURLY':
        return nvl(
          contract.hourlyWage,
        ) * (
          nvl(formModel.workHours)
          + nvl(formModel.overtimeHours)
          + nvl(formModel.holidayWorkHours)
        )

      default:
        return 0
    }
  }

  const recalculateEstimatedPay = () => {
    const basePayAmount =
      calculateBasePayAmount(
        contractQuery.contract.value,
      )

    const gross =
      basePayAmount
      + nvl(formModel.allowanceAmount)

    const net =
      gross
      - nvl(formModel.deductionAmount)
      - nvl(formModel.savingAmount)
      - nvl(
        formModel.loanRepaymentAmount,
      )

    formModel.estimatedGrossPayAmount =
      Math.round(gross)

    formModel.estimatedNetPayAmount =
      Math.round(net)
  }

  const calculateWorkTimes = () => {
    if (applyingDetail.value) {
      return
    }

    const result =
      calculateDailyReportWorkTimes({
        startTime:
          formModel.startTime,

        endTime:
          formModel.endTime,

        breakMinutes:
          formModel.breakMinutes,
      })

    if (!result) {
      recalculateEstimatedPay()
      return
    }

    formModel.workHours =
      result.workHours

    formModel.overtimeHours =
      result.overtimeHours

    formModel.nightWorkHours =
      result.nightWorkHours

    /*
     * 休日時間は現在手入力。
     * 会社カレンダー対応後に自動振分する。
     */
    formModel.holidayWorkHours =
      nvl(formModel.holidayWorkHours)

    recalculateEstimatedPay()
  }

  const applyDefaultInputItems = () => {
    formModel.allowances =
      inputItems.value.allowances.map(
        item => ({
          ...item,
          amount: item.amount ?? 0,
        }),
      )

    formModel.deductions =
      inputItems.value.deductions.map(
        item => ({
          ...item,
          amount: item.amount ?? 0,
        }),
      )
  }

  const applySavedOrDefaultInputItems =
    () => {
      if (
        formModel.allowances.length
        === 0
      ) {
        formModel.allowances =
          inputItems.value.allowances.map(
            item => ({
              ...item,
              amount:
                item.amount ?? 0,
            }),
          )
      }

      if (
        formModel.deductions.length
        === 0
      ) {
        formModel.deductions =
          inputItems.value.deductions.map(
            item => ({
              ...item,
              amount:
                item.amount ?? 0,
            }),
          )
      }
    }

  const applyCustomerSnapshot = () => {
    const customer =
      customerStore.findCustomer(
        formModel.customerId,
      )

    formModel.customerName =
      customer?.name
      ?? formModel.customerName
      ?? ''

    const site =
      customerStore.findSite(
        formModel.customerSiteId,
      )

    formModel.siteName =
      site?.name
      ?? formModel.siteName
      ?? ''
  }

  const resetForm = () => {
    applyingDetail.value = true

    Object.assign(
      formModel,
      createEmptyDailyReportForm(),
    )

    if (
      createParams.value?.employeeId
    ) {
      formModel.employeeId =
        createParams.value.employeeId
    }

    if (
      createParams.value?.workDate
    ) {
      formModel.workDate =
        createParams.value.workDate
    }

    activeTab.value = 'basic'

    applyDefaultInputItems()

    applyingDetail.value = false

    calculateWorkTimes()
    recalculateEstimatedPay()
  }

  watch(
    () => visible.value,
    async opened => {
      if (!opened) {
        return
      }

      await customerStore.load()

      if (!dailyReport.value) {
        resetForm()
      }
    },
    {
      immediate: true,
    },
  )

  watch(
    () => dailyReport.value,
    async value => {
      if (!visible.value) {
        return
      }

      await customerStore.load()

      if (!value) {
        resetForm()
        return
      }

      applyingDetail.value = true

      Object.assign(
        formModel,
        toDailyReportForm(value),
      )

      formModel.holidayWorkHours =
        nvl(formModel.holidayWorkHours)

      formModel.billingHolidayUnitPrice =
        nvl(
          formModel
            .billingHolidayUnitPrice,
        )

      applyCustomerSnapshot()
      applySavedOrDefaultInputItems()

      activeTab.value = 'basic'

      applyingDetail.value = false

      /*
       * 編集データの保存済み時間を維持する。
       */
      recalculateEstimatedPay()
    },
    {
      immediate: true,
    },
  )

  watch(
    () => inputItems.value,
    () => {
      if (!visible.value) {
        return
      }

      if (!dailyReport.value) {
        applyDefaultInputItems()
      } else {
        applySavedOrDefaultInputItems()
      }

      recalculateEstimatedPay()
    },
    {
      deep: true,
      immediate: true,
    },
  )

  watch(
    () => contractQuery.contract.value,
    () => {
      if (!visible.value) {
        return
      }

      recalculateEstimatedPay()
    },
    {
      immediate: true,
    },
  )

  watch(
    () => financeQuery.summary.value,
    summary => {
      if (
        !visible.value
        || !summary
      ) {
        return
      }

      formModel.loanBalance =
        summary.loanBalance ?? 0

      formModel.savingBalance =
        summary.savingBalance ?? 0

      formModel.monthlyLoanRepayment =
        summary.monthlyLoanRepayment
        ?? 0

      formModel.monthlySavingAmount =
        summary.monthlySavingAmount
        ?? 0

      if (formModel.id === 0) {
        formModel.loanRepaymentAmount =
          summary.monthlyLoanRepayment
          ?? 0

        formModel.savingAmount =
          summary.monthlySavingAmount
          ?? 0
      }

      recalculateEstimatedPay()
    },
    {
      immediate: true,
    },
  )

  watch(
    () => formModel.customerId,
    (
      customerId,
      oldCustomerId,
    ) => {
      const customer =
        customerStore.findCustomer(
          customerId,
        )

      formModel.customerName =
        customer?.name ?? ''

      if (applyingDetail.value) {
        return
      }

      if (
        customerId
        !== oldCustomerId
      ) {
        formModel.customerSiteId = null
        formModel.siteName = ''

        clearBillingSelection()
      }
    },
  )

  watch(
    () => formModel.customerSiteId,
    (
      siteId,
      oldSiteId,
    ) => {
      const site =
        customerStore.findSite(siteId)

      formModel.siteName =
        site?.name ?? ''

      if (applyingDetail.value) {
        return
      }

      if (siteId !== oldSiteId) {
        clearBillingSelection()
      }
    },
  )

  watch(
    () => [
      formModel.startTime,
      formModel.endTime,
      formModel.breakMinutes,
    ],
    calculateWorkTimes,
  )

  watch(
    () => [
      formModel.employeeId,
      formModel.workHours,
      formModel.overtimeHours,
      formModel.holidayWorkHours,
      formModel.allowanceAmount,
      formModel.deductionAmount,
      formModel.savingAmount,
      formModel.loanRepaymentAmount,
    ],
    recalculateEstimatedPay,
  )

  watch(
    () =>
      formModel.allowances.map(
        item => item.amount,
      ),
    () => {
      formModel.allowanceAmount =
        formModel.allowances.reduce(
          (
            sum,
            item,
          ) =>
            sum
            + Number(
              item.amount ?? 0,
            ),
          0,
        )

      recalculateEstimatedPay()
    },
    {
      deep: true,
    },
  )

  watch(
    () =>
      formModel.deductions.map(
        item => item.amount,
      ),
    () => {
      formModel.deductionAmount =
        formModel.deductions.reduce(
          (
            sum,
            item,
          ) =>
            sum
            + Number(
              item.amount ?? 0,
            ),
          0,
        )

      recalculateEstimatedPay()
    },
    {
      deep: true,
    },
  )

  watch(
    () => [
      formModel.paidLeaveDays,
      formModel.paidLeaveRemainingDays,
    ],
    () => {
      formModel
        .paidLeaveRemainingAfterUsedDays =
          nvl(
            formModel
              .paidLeaveRemainingDays,
          )
          - nvl(
            formModel.paidLeaveDays,
          )
    },
    {
      immediate: true,
    },
  )

  const isEdit = computed(
    () => formModel.id > 0,
  )

  const tabs: Array<{
    label: string
    value: DailyReportTab
  }> = [
    {
      label: '基本情報',
      value: 'basic',
    },
    {
      label: '請求情報',
      value: 'billing',
    },
    {
      label: '手当',
      value: 'allowance',
    },
    {
      label: '控除',
      value: 'deduction',
    },
    {
      label: '貯蓄・借入',
      value: 'finance',
    },
    {
      label: '承認',
      value: 'approval',
    },
  ]

  const close = () => {
    visible.value = false
  }

  const save = () => {
    formModel.allowanceAmount =
      formModel.allowances.reduce(
        (
          sum,
          item,
        ) =>
          sum
          + Number(
            item.amount ?? 0,
          ),
        0,
      )

    formModel.deductionAmount =
      formModel.deductions.reduce(
        (
          sum,
          item,
        ) =>
          sum
          + Number(
            item.amount ?? 0,
          ),
        0,
      )

    formModel.holidayWorkHours =
      nvl(formModel.holidayWorkHours)

    recalculateEstimatedPay()
    applyCustomerSnapshot()

    if (
      formModel.customerSiteId
      != null
    ) {
      applyBillingRatePreview()
    } else {
      clearBillingSelection()
    }

    emitSave({
      ...formModel,
    })
  }

  const remove = () => {
    emitDelete({
      ...formModel,
    })
  }

  const footerItems = computed<
    ToolbarItem[]
  >(() => {
    const items: ToolbarItem[] = []

    if (isEdit.value) {
      items.push({
        type: 'button',
        label: '削除',
        color: 'error',
        onClick: remove,
      })
    }

    items.push(
      {
        type: 'button',
        label: '閉じる',
        color: 'secondary',
        onClick: close,
      },
      {
        type: 'button',
        label: '保存',
        color: 'primary',
        onClick: save,
      },
    )

    return items
  })

  return {
    activeTab,
    formModel,
    isEdit,

    tabs,
    fields,
    billingFields,
    financeFields,

    schema: dailyReportSchema,
    footerItems,

    billingRateLoading,

    applicableSiteBillingRates:
      applicableBillingRates,

    selectedApplicableBillingRate,

    jobOptions,
    siteRoleOptions,
  }
}