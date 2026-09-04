import {
  computed,
  reactive,
  ref,
  watch,
  onBeforeUnmount,
  nextTick,
  type Ref,
} from 'vue'

import {
  storeToRefs,
} from 'pinia'

import type {
  ToolbarItem,
} from '@/shared/ui/toolbar/types'

import type {
  DailyReportDetailResponse,
} from '@/features/dailyreport/types/dailyReportApiTypes'

import type {
  DailyReportForm,
} from '@/features/dailyreport/types/dailyReportFormTypes'

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
  isWeekendDate,
} from '@/features/dailyreport/utils/dailyReportTimeCalculator'

import {
  dailyReportSchema,
} from '@/features/dailyreport/schemas/dailyReportSchema'
import { useDailyReportInputItemsPreviewMutation } from '@/features/dailyreport/api/useDailyReportInputItemsPreviewMutation'
import { useDailyReportEstimatedPayPreviewMutation } from '@/features/dailyreport/api/useDailyReportEstimatedPayPreviewMutation'
import { toDailyReportSaveRequest } from '@/features/dailyreport/utils/dailyReportConverters'
import { fetchDailyReportPreparationDefaults } from '@/features/dailyreport/api/fetchDailyReportPreparationDefaults'

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

export const useDailyReportEditDialog = (
  visible: Ref<boolean>,
  dailyReport: Ref<DailyReportDetailResponse | null>,
  createParams: Ref<DailyReportCreateParams | null>,
  employees: Ref<EmployeeListItemResponse[]>,
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

  const applyingPayrollPreview =
    ref(false)

  const payrollItemsError =
    ref('')

  const preparationDefaultsMessage =
    ref('')

  const applyingPreparationDefaults =
    ref(false)

  let preparationDefaultsSequence = 0

  let appliedPreparationDefaults: {
    employeeId: number
    workDate: string
    customerId: number | null
    customerSiteId: number | null
    workDescription: string
  } | null = null

  const payrollItemsPreview =
    useDailyReportInputItemsPreviewMutation()

  const estimatedPayPreview =
    useDailyReportEstimatedPayPreviewMutation()

  const payrollItemsLoading =
    computed(() =>
      payrollItemsPreview.isPending.value
      || estimatedPayPreview.isPending.value,
    )

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
    workDate: computed(() => formModel.workDate),
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
        workDate:
          formModel.workDate,

        startTime:
          formModel.startTime,

        endTime:
          formModel.endTime,

        breakMinutes:
          formModel.breakMinutes,

        holidayPremiumEligible:
          formModel.holidayPremiumEligible,
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

    formModel.holidayWorkHours =
      result.holidayWorkHours

    recalculateEstimatedPay()
  }

  let previewTimer:
    ReturnType<typeof setTimeout>
    | undefined

  let previewSequence = 0

  const applyPreviewItems = (
    response: Awaited<
      ReturnType<
        typeof payrollItemsPreview.mutateAsync
      >
    >,
  ) => {
    applyingPayrollPreview.value = true

    try {
      formModel.allowances =
        response.allowances.map(item => ({
          ...item,
          amount: item.amount ?? 0,
          overrideReason: item.overrideReason ?? '',
        }))

      formModel.deductions =
        response.deductions.map(item => ({
          ...item,
          amount: item.amount ?? 0,
          overrideReason: item.overrideReason ?? '',
        }))
    } finally {
      applyingPayrollPreview.value = false
    }
  }

  const previewPayrollItems =
    async () => {
      if (
        !visible.value
        || applyingDetail.value
        || formModel.employeeId == null
        || !formModel.workDate.trim()
      ) {
        return
      }

      const sequence =
        ++previewSequence

      payrollItemsError.value = ''

      try {
        const response =
          await payrollItemsPreview
            .mutateAsync(
              toDailyReportSaveRequest(
                formModel,
              ),
            )

        if (
          sequence
          !== previewSequence
        ) {
          return
        }

        applyPreviewItems(response)

        const estimated =
          await estimatedPayPreview.mutateAsync(
            toDailyReportSaveRequest(formModel),
          )

        if (sequence !== previewSequence) {
          return
        }

        formModel.estimatedGrossPayAmount =
          Number(estimated.estimatedGrossPayAmount ?? 0)
        formModel.estimatedNetPayAmount =
          Number(estimated.estimatedNetPayAmount ?? 0)
      } catch (error) {
        if (
          sequence
          !== previewSequence
        ) {
          return
        }

        payrollItemsError.value =
          error instanceof Error
            ? error.message
            : '手当・控除の自動計算に失敗しました。'
      }
    }

  const schedulePayrollItemPreview =
    () => {
      if (previewTimer) {
        clearTimeout(previewTimer)
      }

      previewTimer = setTimeout(
        () => {
          void previewPayrollItems()
        },
        400,
      )
    }

  onBeforeUnmount(() => {
    if (previewTimer) {
      clearTimeout(previewTimer)
    }
  })

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

    preparationDefaultsSequence += 1
    appliedPreparationDefaults = null
    preparationDefaultsMessage.value = ''

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
      formModel.holidayPremiumEligible =
        isWeekendDate(formModel.workDate)
    }

    applyingDetail.value = false

    calculateWorkTimes()
    recalculateEstimatedPay()
    schedulePayrollItemPreview()
  }

  const applyPreparationDefaults = async () => {
    if (
      !visible.value
      || formModel.id !== 0
      || formModel.employeeId == null
      || !formModel.workDate.trim()
    ) {
      preparationDefaultsMessage.value = ''
      return
    }

    if (appliedPreparationDefaults) {
      const previous = appliedPreparationDefaults
      const previousValuesAreUnchanged =
        formModel.customerId === previous.customerId
        && formModel.customerSiteId === previous.customerSiteId
        && formModel.workDescription === previous.workDescription

      if (
        previous.employeeId === formModel.employeeId
        && previous.workDate === formModel.workDate
      ) {
        return
      }

      if (!previousValuesAreUnchanged) {
        appliedPreparationDefaults = null
        preparationDefaultsMessage.value = ''
        return
      }

      applyingDetail.value = true
      try {
        formModel.customerId = null
        formModel.customerSiteId = null
        formModel.customerName = ''
        formModel.siteName = ''
        formModel.workDescription = ''
        await nextTick()
      } finally {
        applyingDetail.value = false
      }

      appliedPreparationDefaults = null
      preparationDefaultsMessage.value = ''
    }

    if (
      formModel.customerId != null
      || formModel.customerSiteId != null
      || formModel.workDescription.trim()
    ) {
      return
    }

    const employeeId = formModel.employeeId
    const workDate = formModel.workDate
    const sequence = ++preparationDefaultsSequence
    const defaults = await fetchDailyReportPreparationDefaults(
      workDate,
      employeeId,
    ).catch(() => null)

    if (
      sequence !== preparationDefaultsSequence
      || employeeId !== formModel.employeeId
      || workDate !== formModel.workDate
    ) {
      return
    }

    if (defaults == null || !defaults.available) {
      preparationDefaultsMessage.value = ''
      return
    }

    applyingPreparationDefaults.value = true
    applyingDetail.value = true
    try {
      formModel.customerId = defaults.customerId
      formModel.customerSiteId = defaults.customerSiteId
      formModel.customerName = defaults.customerName ?? ''
      formModel.siteName = defaults.siteName ?? ''
      formModel.workDescription = defaults.workDescription ?? ''
      appliedPreparationDefaults = {
        employeeId,
        workDate,
        customerId: defaults.customerId,
        customerSiteId: defaults.customerSiteId,
        workDescription: defaults.workDescription ?? '',
      }
      preparationDefaultsMessage.value =
        '翌日準備の顧客・現場・作業内容を初期値へ反映しました。実績に合わせて変更できます。'
      await nextTick()
    } finally {
      applyingDetail.value = false
      applyingPreparationDefaults.value = false
    }
  }

  watch(
    () => visible.value,
    async opened => {
      if (!opened) {
        return
      }

      // ダイアログを開いた時点でだけ初期タブへ戻す。
      // 非同期の詳細取得完了後に戻すと、利用者が選択したタブを
      // 後から基本情報へ上書きしてしまうため、ここで先に確定する。
      activeTab.value = 'basic'

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

      applyingDetail.value = false

      /*
       * 編集データの保存済み時間を維持する。
       */
      recalculateEstimatedPay()
      schedulePayrollItemPreview()
    },
    {
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
        // 日報には、その日に実際に受領した金額だけを入力する。
        // 月返済・月積立の予定額は参考情報として別項目に保持する。
        formModel.loanRepaymentAmount = 0
        formModel.savingAmount = 0
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
      formModel.employeeId,
      formModel.workDate,
      formModel.id,
    ],
    () => {
      if (applyingPreparationDefaults.value) {
        return
      }
      void applyPreparationDefaults()
    },
  )

  watch(
    () => formModel.workDate,
    (workDate, previousWorkDate) => {
      if (
        applyingDetail.value
        || workDate === previousWorkDate
      ) {
        return
      }

      formModel.holidayPremiumEligible =
        isWeekendDate(workDate)
    },
  )

  watch(
    () => [
      formModel.workDate,
      formModel.startTime,
      formModel.endTime,
      formModel.breakMinutes,
      formModel.holidayPremiumEligible,
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
    () => [
      formModel.employeeId,
      formModel.workDate,
      formModel.paymentDate,
      formModel.customerId,
      formModel.customerSiteId,
      formModel.jobCode,
      formModel.siteRoleCode,
      formModel.workHours,
      formModel.overtimeHours,
      formModel.nightWorkHours,
      formModel.holidayWorkHours,
      formModel.vehicleUsedFlag,
      formModel.mileage,
      formModel.paidLeaveDays,
    ],
    schedulePayrollItemPreview,
  )

  watch(
    () => formModel.deductions.map(
      item => `${item.masterId}:${item.quantity}`,
    ),
    () => {
      if (applyingPayrollPreview.value) {
        return
      }

      schedulePayrollItemPreview()
    },
    {
      flush: 'sync',
    },
  )

  watch(
    () => formModel.allowances.map(
      item => `${item.masterId}:${item.quantity}`,
    ),
    () => {
      if (applyingPayrollPreview.value) {
        return
      }

      schedulePayrollItemPreview()
    },
    {
      flush: 'sync',
    },
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

  const leftFooterItems = computed<
    ToolbarItem[]
  >(() => {
    const items: ToolbarItem[] = []

    if (isEdit.value) {
      items.push({
        type: 'button',
        label: '削除',
        intent: 'danger',
        onClick: remove,
      })
    }

    return items
  })

  const rightFooterItems = computed<
    ToolbarItem[]
  >(() => [
      {
        type: 'button',
        label: '閉じる',
        intent: 'secondary',
        onClick: close,
      },
      {
        type: 'button',
        label: '保存',
        intent: 'primary',
        disabled:
          payrollItemsLoading.value
          || billingRateLoading.value,
        onClick: save,
      },
    ])

  return {
    activeTab,
    formModel,
    isEdit,

    tabs,
    fields,
    billingFields,
    financeFields,

    schema: dailyReportSchema,
    leftFooterItems,
    rightFooterItems,

    billingRateLoading,
    payrollItemsLoading,
    payrollItemsError,
    preparationDefaultsMessage,

    applicableSiteBillingRates:
      applicableBillingRates,

    selectedApplicableBillingRate,

    jobOptions,
    siteRoleOptions,
  }
}
