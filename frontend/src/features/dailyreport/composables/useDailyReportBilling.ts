import {
  computed,
  watch,
  type Ref,
} from 'vue'

import type {
  CustomerSiteBillingRateResponse,
} from '@/features/customer/types/customerApiTypes'

import { useCustomerSiteBillingRatesQuery } from '@/features/customer/api/useCustomerSiteBillingRatesQuery'

import type {
  DailyReportForm,
} from '@/features/dailyreport/types/dailyReportFormTypes'

type SelectOption<T = string> = {
  title: string
  value: T
}

type UseDailyReportBillingOptions = {
  visible: Ref<boolean>
  applyingDetail: Ref<boolean>
  formModel: DailyReportForm
}

export const useDailyReportBilling = ({
  visible,
  applyingDetail,
  formModel,
}: UseDailyReportBillingOptions) => {
  /*
   * APIは現場ID単位。
   */
  const billingRateQuery =
    useCustomerSiteBillingRatesQuery(
      computed(
        () => formModel.customerSiteId,
      ),
    )

  const billingRates = computed<
    CustomerSiteBillingRateResponse[]
  >(() =>
    billingRateQuery.billingRates.value
    ?? [],
  )

  const isApplicableOnWorkDate = (
    rate: CustomerSiteBillingRateResponse,
  ): boolean => {
    if (rate.activeFlag === false) {
      return false
    }

    if (!formModel.workDate) {
      return true
    }

    if (
      rate.effectiveFrom
      && rate.effectiveFrom
        > formModel.workDate
    ) {
      return false
    }

    if (
      rate.effectiveTo
      && rate.effectiveTo
        < formModel.workDate
    ) {
      return false
    }

    return true
  }

  const applicableBillingRates = computed<
    CustomerSiteBillingRateResponse[]
  >(() =>
    billingRates.value.filter(
      isApplicableOnWorkDate,
    ),
  )

  const jobOptions = computed<
    SelectOption<string>[]
  >(() => {
    const optionMap =
      new Map<
        string,
        SelectOption<string>
      >()

    applicableBillingRates.value.forEach(
      rate => {
        const code =
          rate.jobCode?.trim()

        if (!code) {
          return
        }

        const name =
          rate.jobName?.trim()

        optionMap.set(code, {
          title: name
            ? `${code} / ${name}`
            : code,
          value: code,
        })
      },
    )

    return [
      ...optionMap.values(),
    ]
  })

  const siteRoleOptions = computed<
    SelectOption<string>[]
  >(() => {
    const optionMap =
      new Map<
        string,
        SelectOption<string>
      >()

    applicableBillingRates.value
      .filter(
        rate =>
          rate.jobCode
          === formModel.jobCode,
      )
      .forEach(rate => {
        const code =
          rate.siteRoleCode?.trim()
          || 'GENERAL'

        const name =
          rate.siteRoleName?.trim()
          || '一般'

        optionMap.set(code, {
          title: `${code} / ${name}`,
          value: code,
        })
      })

    return [
      ...optionMap.values(),
    ]
  })

  const selectedApplicableBillingRate =
    computed<
      CustomerSiteBillingRateResponse | null
    >(() => {
      if (
        formModel.customerSiteId == null
        || !formModel.jobCode
      ) {
        return null
      }

      const roleCode =
        formModel.siteRoleCode?.trim()
        || 'GENERAL'

      return applicableBillingRates.value
        .filter(rate => {
          const rateRoleCode =
            rate.siteRoleCode?.trim()
            || 'GENERAL'

          return (
            rate.jobCode
              === formModel.jobCode
            && rateRoleCode
              === roleCode
          )
        })
        .sort((left, right) =>
          String(
            right.effectiveFrom ?? '',
          ).localeCompare(
            String(
              left.effectiveFrom ?? '',
            ),
          ),
        )[0] ?? null
    })

  const clearBillingPreview = () => {
    formModel.billingRateId = null
    formModel.billingUnit = null

    formModel.billingBaseUnitPrice = 0
    formModel.billingOvertimeUnitPrice = 0
    formModel.billingNightUnitPrice = 0
    formModel.billingHolidayUnitPrice = 0
    formModel.billingCommuteUnitPrice = 0
  }

  const clearBillingSelection = () => {
    formModel.jobCode = ''
    formModel.jobName = ''

    formModel.siteRoleCode =
      'GENERAL'

    formModel.siteRoleName =
      '一般'

    clearBillingPreview()
  }

  const applyBillingRatePreview = () => {
    const rate =
      selectedApplicableBillingRate.value

    if (!rate) {
      clearBillingPreview()
      return
    }

    formModel.billingRateId =
      rate.id

    formModel.jobCode =
      rate.jobCode ?? ''

    formModel.jobName =
      rate.jobName ?? ''

    formModel.siteRoleCode =
      rate.siteRoleCode
      || 'GENERAL'

    formModel.siteRoleName =
      rate.siteRoleName
      || '一般'

    formModel.billingUnit =
      rate.billingUnit ?? null

    formModel.billingBaseUnitPrice =
      Number(
        rate.baseUnitPrice ?? 0,
      )

    formModel.billingOvertimeUnitPrice =
      Number(
        rate.overtimeUnitPrice ?? 0,
      )

    formModel.billingNightUnitPrice =
      Number(
        rate.nightUnitPrice ?? 0,
      )

    formModel.billingHolidayUnitPrice =
      Number(
        rate.holidayUnitPrice ?? 0,
      )

    formModel.billingCommuteUnitPrice =
      Number(
        rate.commuteUnitPrice ?? 0,
      )
  }

  watch(
    () => formModel.jobCode,
    (
      jobCode,
      oldJobCode,
    ) => {
      const selectedRate =
        applicableBillingRates.value.find(
          rate =>
            rate.jobCode === jobCode,
        )

      formModel.jobName =
        selectedRate?.jobName ?? ''

      if (applyingDetail.value) {
        return
      }

      if (jobCode !== oldJobCode) {
        const currentRoleExists =
          siteRoleOptions.value.some(
            option =>
              option.value
              === formModel.siteRoleCode,
          )

        if (!currentRoleExists) {
          formModel.siteRoleCode =
            siteRoleOptions.value[0]?.value
            ?? 'GENERAL'
        }
      }

      applyBillingRatePreview()
    },
  )

  watch(
    () => formModel.siteRoleCode,
    siteRoleCode => {
      const normalizedRoleCode =
        siteRoleCode || 'GENERAL'

      const selectedRate =
        applicableBillingRates.value.find(
          rate => {
            const rateRoleCode =
              rate.siteRoleCode
              || 'GENERAL'

            return (
              rate.jobCode
                === formModel.jobCode
              && rateRoleCode
                === normalizedRoleCode
            )
          },
        )

      formModel.siteRoleName =
        selectedRate?.siteRoleName
        ?? (
          normalizedRoleCode
            === 'GENERAL'
            ? '一般'
            : ''
        )

      if (applyingDetail.value) {
        return
      }

      applyBillingRatePreview()
    },
  )

  watch(
    () => formModel.workDate,
    () => {
      if (applyingDetail.value) {
        return
      }

      applyBillingRatePreview()
    },
  )

  watch(
    billingRates,
    () => {
      if (
        !visible.value
        || applyingDetail.value
      ) {
        return
      }

      applyBillingRatePreview()
    },
    {
      deep: true,
    },
  )

  return {
    billingRateLoading:
      billingRateQuery.isFetching,

    billingRates,
    applicableBillingRates,

    jobOptions,
    siteRoleOptions,

    selectedApplicableBillingRate,

    clearBillingPreview,
    clearBillingSelection,
    applyBillingRatePreview,
  }
}