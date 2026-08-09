import { watch, type Ref } from 'vue'

import { useDailyReportInputItemsQuery } from '@/features/dailyreport/api/useDailyReportInputItemsQuery'
import type { DailyReportForm } from '@/features/dailyreport/types/dailyReportFormTypes'
import type {
  DailyReportAmountItemForm,
  DailyReportInputItemResponse,
} from '@/features/dailyreport/types/dailyReportInputItemTypes'

const toAmountItemForm = (
  item: DailyReportInputItemResponse,
): DailyReportAmountItemForm => ({
  masterId: item.masterId,
  code: item.code,
  name: item.name,
  itemType: item.itemType,
  inputMode: item.inputMode,
  calculatedAmount: item.calculatedAmount ?? item.amount ?? 0,
  amount: item.amount ?? 0,
  manualOverride: item.manualOverride ?? false,
  overrideReason: item.overrideReason ?? '',
  editable: item.editable,
  displayOrder: item.displayOrder,
  balanceTracked: item.balanceTracked ?? false,
  balanceUnit: item.balanceUnit ?? null,
  openingQuantity: item.openingQuantity ?? 0,
  accruedQuantity: item.accruedQuantity ?? 0,
  consumedQuantity: item.consumedQuantity ?? 0,
  remainingQuantity: item.remainingQuantity ?? 0,
  quantity: item.quantity ?? 0,
  remainingAfterQuantity: item.remainingAfterQuantity ?? 0,
})

export const useDailyReportInputItems = (
  form: Ref<DailyReportForm>,
  enabled: Ref<boolean>,
) => {
  const query = useDailyReportInputItemsQuery()

  watch(
    () => query.inputItems.value,
    (data) => {
      if (!enabled.value) {
        return
      }

      if (form.value.employeeId == null || !form.value.workDate.trim()) {
        return
      }

      form.value.allowances = data.allowances.map(toAmountItemForm)
      form.value.deductions = data.deductions.map(toAmountItemForm)
    },
    { immediate: true },
  )

  return query
}
