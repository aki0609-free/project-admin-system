import { computed, ref } from 'vue'

import type { DailyReportDetailResponse } from '@/features/dailyreport/types/dailyReportApiTypes'
import type { DailyReportForm } from '@/features/dailyreport/types/dailyReportFormTypes'
import {
  createEmptyDailyReportForm,
  toDailyReportForm,
} from '@/features/dailyreport/utils/dailyReportFormFactory'

export const useDailyReportForm = () => {
  const form = ref<DailyReportForm>(createEmptyDailyReportForm())

  const isEdit = computed(() => form.value.id > 0)

  const resetForm = () => {
    form.value = createEmptyDailyReportForm()
  }

  const setForm = (detail: DailyReportDetailResponse) => {
    form.value = toDailyReportForm(detail)
  }

  const updateEstimatedPay = (
    estimatedGrossPayAmount: number,
    estimatedNetPayAmount: number,
  ) => {
    form.value.estimatedGrossPayAmount = estimatedGrossPayAmount
    form.value.estimatedNetPayAmount = estimatedNetPayAmount
  }

  const updatePaidLeaveBalance = (
    paidLeaveRemainingDays: number,
    paidLeaveRemainingAfterUsedDays: number,
  ) => {
    form.value.paidLeaveRemainingDays = paidLeaveRemainingDays
    form.value.paidLeaveRemainingAfterUsedDays = paidLeaveRemainingAfterUsedDays
  }

  return {
    form,
    isEdit,
    resetForm,
    setForm,
    updateEstimatedPay,
    updatePaidLeaveBalance,
  }
}